package com.legalconnect.lawyerbooking.service;

import com.legalconnect.lawyerbooking.entity.Case;
import com.legalconnect.lawyerbooking.entity.Appointment;
import com.legalconnect.lawyerbooking.entity.Message;
import com.legalconnect.lawyerbooking.exception.UnauthorizedException;
import com.legalconnect.lawyerbooking.exception.ResourceNotFoundException;
import com.legalconnect.lawyerbooking.repository.CaseRepository;
import com.legalconnect.lawyerbooking.repository.AppointmentRepository;
import com.legalconnect.lawyerbooking.repository.MessageRepository;
import com.legalconnect.lawyerbooking.repository.LawyerRepository;
import com.legalconnect.lawyerbooking.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for handling authorization checks
 */
@Service
public class AuthorizationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationService.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private LawyerRepository lawyerRepository;

    public com.legalconnect.lawyerbooking.security.UserPrincipal getCurrentUser() {
        return getCurrentUserOrDefault(null);
    }

    private com.legalconnect.lawyerbooking.security.UserPrincipal getCurrentUserOrDefault(java.security.Principal principal) {
        if (principal instanceof com.legalconnect.lawyerbooking.security.UserPrincipal) {
            return (com.legalconnect.lawyerbooking.security.UserPrincipal) principal;
        }

        // Handle STOMP principal which is often a UsernamePasswordAuthenticationToken
        if (principal instanceof org.springframework.security.core.Authentication) {
            Object authPrincipal = ((org.springframework.security.core.Authentication) principal).getPrincipal();
            if (authPrincipal instanceof com.legalconnect.lawyerbooking.security.UserPrincipal) {
                return (com.legalconnect.lawyerbooking.security.UserPrincipal) authPrincipal;
            }
        }

        org.springframework.security.core.Authentication auth = 
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.legalconnect.lawyerbooking.security.UserPrincipal) {
            return (com.legalconnect.lawyerbooking.security.UserPrincipal) auth.getPrincipal();
        }
        throw new com.legalconnect.lawyerbooking.exception.UnauthorizedException("User not authenticated");
    }

    /**
     * Verifies that the current user/lawyer has access to a specific case
     */
    public void verifyCaseAccess(Long caseId) {
        com.legalconnect.lawyerbooking.security.UserPrincipal currentUser = getCurrentUser();
        Long userId = currentUser.getUserId();
        String userType = currentUser.getRole();

        Case caseEntity = caseRepository.findById(caseId)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found with id: " + caseId));

        if (userType.equalsIgnoreCase("user")) {
            if (!caseEntity.getUserId().equals(userId)) {
                logger.warn("ACCESS DENIED: User {} (type: {}) attempted to access case {} owned by user {}. Principal: {}", 
                           userId, userType, caseId, caseEntity.getUserId(), currentUser.getName());
                throw new UnauthorizedException("You can only access your own cases");
            }
        } else if (userType.equalsIgnoreCase("lawyer")) {
            // 1. If assigned, only the assigned lawyer can access
            if (caseEntity.getLawyerId() != null) {
                if (!caseEntity.getLawyerId().equals(userId)) {
                    logger.warn("ACCESS DENIED: Lawyer {} attempted to access case {} assigned to lawyer {}", 
                               userId, caseId, caseEntity.getLawyerId());
                    throw new UnauthorizedException("You can only access cases assigned to you");
                }
            } else {
                // 2. If UNASSIGNED, only lawyers with matching specialization can access
                var lawyer = lawyerRepository.findById(userId)
                    .orElseThrow(() -> new UnauthorizedException("Lawyer profile not found"));
                
                com.legalconnect.lawyerbooking.enums.CaseType type = caseEntity.getCaseType();
                java.util.Set<com.legalconnect.lawyerbooking.enums.CaseType> specs = lawyer.getSpecializations();
                
                if (type == null || specs == null || !specs.contains(type)) {
                    logger.warn("ACCESS DENIED: Lawyer {} (specs: {}) attempted to access unassigned case {} of type {}", 
                               userId, specs, caseId, type);
                    throw new UnauthorizedException("You can only access cases that match your specializations");
                }
            }
            // 3. Ensure case is not deleted (Soft Delete)
            if (Boolean.TRUE.equals(caseEntity.getDeleted())) {
                throw new ResourceNotFoundException("Case not found or deleted");
            }
            
            logger.info("Access granted for lawyer {} to case {}", userId, caseId);
        } else {
            logger.error("Invalid user type: {} for user {}", userType, userId);
            throw new UnauthorizedException("Invalid user type");
        }
    }

    /**
     * Verifies that the current user/lawyer can send a message for a specific case
     */
    public void verifyMessageAccess(Long caseId) {
        verifyMessageAccess(caseId, null);
    }

    /**
     * Verifies that a specific principal can send a message for a specific case
     */
    public void verifyMessageAccess(Long caseId, java.security.Principal principal) {
        com.legalconnect.lawyerbooking.security.UserPrincipal currentUser = getCurrentUserOrDefault(principal);
        Long senderId = currentUser.getUserId();
        String senderType = currentUser.getRole();

        Case caseEntity = caseRepository.findById(caseId)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found with id: " + caseId));

        if (senderType.equalsIgnoreCase("user")) {
            if (!caseEntity.getUserId().equals(senderId)) {
                throw new UnauthorizedException("User does not have access to this case");
            }
            // Restriction: User cannot message if case is pending approval
            if (caseEntity.getCaseStatus() == com.legalconnect.lawyerbooking.enums.CaseStatus.PENDING_APPROVAL) {
                logger.warn("BLOCKED: User {} attempted to message for pending case {}", senderId, caseId);
                throw new UnauthorizedException("You must wait for the lawyer to accept the connection request before you can send messages");
            }
        } else if (senderType.equalsIgnoreCase("lawyer")) {
            if (caseEntity.getLawyerId() == null || !caseEntity.getLawyerId().equals(senderId)) {
                throw new UnauthorizedException("Lawyer does not have access to this case");
            }
            // Restriction: Lawyer cannot message if case is pending approval
            if (caseEntity.getCaseStatus() == com.legalconnect.lawyerbooking.enums.CaseStatus.PENDING_APPROVAL) {
                logger.warn("BLOCKED: Lawyer {} attempted to message for pending case {}", senderId, caseId);
                throw new UnauthorizedException("You must accept the connection request before you can send messages");
            }
        } else {
            throw new UnauthorizedException("Invalid sender type");
        }
    }

    /**
     * Verifies that a user can access an appointment
     */
    public void verifyAppointmentAccess(Long appointmentId) {
        com.legalconnect.lawyerbooking.security.UserPrincipal currentUser = getCurrentUser();
        Long userId = currentUser.getUserId();
        String userType = currentUser.getRole();

        Appointment appointment = appointmentRepository.findById(appointmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

        if (userType.equalsIgnoreCase("user")) {
            if (!appointment.getUserId().equals(userId)) {
                throw new UnauthorizedException("You can only access your own appointments");
            }
        } else if (userType.equalsIgnoreCase("lawyer")) {
            if (!appointment.getLawyerId().equals(userId)) {
                throw new UnauthorizedException("You can only access your own appointments");
            }
        }
    }

    /**
     * Verifies that a user can update a case (only assigned lawyer can update)
     */
    public void verifyCaseUpdateAccess(Long caseId) {
        com.legalconnect.lawyerbooking.security.UserPrincipal currentUser = getCurrentUser();
        Long userId = currentUser.getUserId();
        String userType = currentUser.getRole();
        

        Case caseEntity = caseRepository.findById(caseId)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found"));

        if (userType.equalsIgnoreCase("lawyer")) {
            if (caseEntity.getLawyerId() == null) {
                logger.warn("Update attempt on unassigned case {} by lawyer {}", caseId, userId);
                throw new UnauthorizedException("You can only update cases assigned to you");
            }
            if (!caseEntity.getLawyerId().equals(userId)) {
                logger.warn("Lawyer {} attempted to update case {} assigned to lawyer {}", 
                           userId, caseId, caseEntity.getLawyerId());
                throw new UnauthorizedException("You can only update cases assigned to you");
            }
            // Restriction: Lawyer cannot update solution if case is pending approval
            if (caseEntity.getCaseStatus() == com.legalconnect.lawyerbooking.enums.CaseStatus.PENDING_APPROVAL) {
                logger.warn("BLOCKED: Lawyer {} attempted to update solution for pending case {}", userId, caseId);
                throw new UnauthorizedException("You must accept the connection request before you can submit a solution");
            }
        } else if (userType.equalsIgnoreCase("user")) {
            if (!caseEntity.getUserId().equals(userId)) {
                logger.warn("User {} attempted to update case {} owned by user {}", 
                           userId, caseId, caseEntity.getUserId());
                throw new UnauthorizedException("You can only update your own cases");
            }
        } else {
            logger.warn("Invalid user role {} for user {} attempting to update case {}", userType, userId, caseId);
            throw new UnauthorizedException("Insufficient permissions to update case");
        }
    }
    /**
     * Verifies that the current user/lawyer can perform an assignment action.
     * Users can assign lawyers to their own cases.
     * Lawyers can claim cases for themselves.
     */
    public void verifyCaseAssignmentAction(Long caseId, Long requestedLawyerId) {
        com.legalconnect.lawyerbooking.security.UserPrincipal currentUser = getCurrentUser();
        Long userId = currentUser.getUserId();
        String role = currentUser.getRole();

        Case caseEntity = caseRepository.findById(caseId)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found with id: " + caseId));

        if (role.equalsIgnoreCase("user")) {
            // User can only assign if they own the case
            if (!caseEntity.getUserId().equals(userId)) {
                logger.warn("ACCESS DENIED: User {} attempted to assign lawyer to case {} owned by user {}", 
                           userId, caseId, caseEntity.getUserId());
                throw new UnauthorizedException("You can only assign lawyers to your own cases");
            }
        } else if (role.equalsIgnoreCase("lawyer")) {
            // Lawyer can only assign to themselves (claim)
            if (!userId.equals(requestedLawyerId)) {
                logger.warn("ACCESS DENIED: Lawyer {} attempted to assign case {} to lawyer {}", 
                           userId, caseId, requestedLawyerId);
                throw new UnauthorizedException("You can only claim cases for yourself");
            }
        } else {
            throw new UnauthorizedException("Insufficient permissions");
        }
    }
    /**
     * Verifies that the current lawyer matches the requested lawyerId for profile updates
     */
    public void verifyLawyerAccess(Long lawyerId) {
        com.legalconnect.lawyerbooking.security.UserPrincipal currentUser = getCurrentUser();
        Long userId = currentUser.getUserId();
        String userType = currentUser.getRole();

        if (!userType.equalsIgnoreCase("lawyer") || !userId.equals(lawyerId)) {
            logger.warn("User {} with type {} attempted to access/update lawyer profile {}", 
                       userId, userType, lawyerId);
            throw new UnauthorizedException("You can only manage your own lawyer profile");
        }
    }
}
