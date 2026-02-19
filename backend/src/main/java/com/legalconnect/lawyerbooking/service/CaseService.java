package com.legalconnect.lawyerbooking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import com.legalconnect.lawyerbooking.entity.Case;
import com.legalconnect.lawyerbooking.enums.CaseStatus;
import com.legalconnect.lawyerbooking.enums.CaseType;
import com.legalconnect.lawyerbooking.exception.BadRequestException;
import com.legalconnect.lawyerbooking.exception.ResourceNotFoundException;
import com.legalconnect.lawyerbooking.exception.UnauthorizedException;
import com.legalconnect.lawyerbooking.repository.CaseRepository;
import com.legalconnect.lawyerbooking.repository.LawyerRepository;
import com.legalconnect.lawyerbooking.repository.ClientAudioRepository;
import com.legalconnect.lawyerbooking.dto.CaseDTO;
import com.legalconnect.lawyerbooking.dto.CaseRequest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CaseService {

    private static final Logger logger = LoggerFactory.getLogger(CaseService.class);

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private LawyerRepository lawyerRepository;

    @Autowired
    private com.legalconnect.lawyerbooking.repository.UserRepository userRepository;

    @Autowired
    private CaseClassificationService classificationService;

    @Autowired
    private ClientAudioRepository clientAudioRepository;

    @Autowired
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private CaseAuditLogService auditLogService;

    @Autowired
    private TextMaskingService textMaskingService;

    @Autowired
    private AuthorizationService authorizationService;

    public CaseDTO createCase(CaseRequest request) {
        Case caseEntity = new Case();
        caseEntity.setUserId(request.getUserId());
        caseEntity.setCaseTitle(request.getCaseTitle());
        caseEntity.setCaseType(request.getCaseType());
        
        // If type is still null, attempt auto-classification
        if (caseEntity.getCaseType() == null) {
            String category = classificationService.classifyCase(request.getDescription());
            try {
                if (category != null && !category.trim().isEmpty()) {
                    String normCategory = category.trim().toUpperCase().replace(" ", "_");
                    caseEntity.setCaseType(com.legalconnect.lawyerbooking.enums.CaseType.valueOf(normCategory));
                } else {
                    caseEntity.setCaseType(com.legalconnect.lawyerbooking.enums.CaseType.OTHER);
                }
            } catch (Exception e) {
                logger.warn("Failed to map classification '{}' to CaseType, defaulting to OTHER", category);
                caseEntity.setCaseType(com.legalconnect.lawyerbooking.enums.CaseType.OTHER);
            }
        }

        caseEntity.setDescription(request.getDescription());
        
        if (request.getLawyerId() != null) {
            caseEntity.setLawyerId(request.getLawyerId());
            // When user creates a case with a specific lawyer, it's pending approval
            caseEntity.setCaseStatus(com.legalconnect.lawyerbooking.enums.CaseStatus.PENDING_APPROVAL);
        } else {
            // New cases start as DRAFT (awaiting user review/publish)
            caseEntity.setCaseStatus(com.legalconnect.lawyerbooking.enums.CaseStatus.DRAFT);
        }
        
        caseEntity.setDeleted(false);
        
        Case saved = caseRepository.save(caseEntity);
        CaseDTO dto = convertToDTO(saved);

        // Audit Log
        try {
            com.legalconnect.lawyerbooking.security.UserPrincipal currentUser = authorizationService.getCurrentUser();
            auditLogService.logEvent(
                saved.getId(),
                "CASE_CREATED",
                null,
                saved.getCaseStatus().name(),
                "Case created as " + saved.getCaseStatus().name(),
                currentUser.getUserId(),
                currentUser.getRole()
            );
        } catch (Exception e) {
            logger.warn("Could not log audit for case creation: {}", e.getMessage());
        }
        
        // Broadcast new case to lawyers ONLY if PUBLISHED (not DRAFT)
        if (saved.getCaseStatus() == CaseStatus.PUBLISHED) {
            try {
                com.legalconnect.lawyerbooking.dto.LawyerCaseRequest requestPayload = new com.legalconnect.lawyerbooking.dto.LawyerCaseRequest(
                    dto.getId(),
                    dto.getCaseTitle(),
                    dto.getCaseType(),
                    dto.getDescription(),
                    dto.getUserId()
                );
                
                if (messagingTemplate != null) {
                    messagingTemplate.convertAndSend("/topic/lawyer/requests", requestPayload);
                    logger.info("Sent new case request for case ID: {}", dto.getId());
                }
            } catch (Exception e) {
                logger.error("Failed to send lawyer request: {}", e.getMessage());
            }
        }
        
        return dto;
    }

    @Transactional
    public CaseDTO publishCase(Long caseId) {
        Case caseEntity = caseRepository.findById(caseId)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found with id: " + caseId));
        
        // Verify the case is in DRAFT status
        if (caseEntity.getCaseStatus() != CaseStatus.DRAFT) {
            throw new BadRequestException("Only DRAFT cases can be published. Current status: " + caseEntity.getCaseStatus());
        }
        
        // Verify user owns this case
        com.legalconnect.lawyerbooking.security.UserPrincipal currentUser = authorizationService.getCurrentUser();
        if (!caseEntity.getUserId().equals(currentUser.getUserId())) {
            throw new UnauthorizedException("You can only publish your own cases");
        }
        
        // Validate minimum required fields
        if (caseEntity.getCaseTitle() == null || caseEntity.getCaseTitle().trim().isEmpty()) {
            throw new BadRequestException("Case title is required before publishing");
        }
        if (caseEntity.getDescription() == null || caseEntity.getDescription().trim().isEmpty()) {
            throw new BadRequestException("Case description is required before publishing");
        }
        
        CaseStatus oldStatus = caseEntity.getCaseStatus();
        caseEntity.setCaseStatus(CaseStatus.PUBLISHED);
        Case saved = caseRepository.save(caseEntity);
        CaseDTO dto = convertToDTO(saved);
        
        // Audit Log
        try {
            auditLogService.logEvent(
                caseId,
                "CASE_PUBLISHED",
                oldStatus.name(),
                CaseStatus.PUBLISHED.name(),
                "Case published and made visible to lawyers",
                currentUser.getUserId(),
                currentUser.getRole()
            );
        } catch (Exception e) {
            logger.warn("Could not log audit for case publication: {}", e.getMessage());
        }
        
        // Broadcast to lawyers now that case is published
        try {
            com.legalconnect.lawyerbooking.dto.LawyerCaseRequest requestPayload = new com.legalconnect.lawyerbooking.dto.LawyerCaseRequest(
                dto.getId(),
                dto.getCaseTitle(),
                dto.getCaseType(),
                dto.getDescription(),
                dto.getUserId()
            );
            
            if (messagingTemplate != null) {
                messagingTemplate.convertAndSend("/topic/lawyer/requests", requestPayload);
                logger.info("Broadcasted published case ID: {} to lawyers", dto.getId());
            }
        } catch (Exception e) {
            logger.error("Failed to broadcast published case: {}", e.getMessage());
        }
        
        return dto;
    }

    @Transactional
    public CaseDTO updateCase(Long caseId, CaseRequest request) {
        Case caseEntity = caseRepository.findById(caseId)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found with id: " + caseId));
        
        // Verify user owns this case
        com.legalconnect.lawyerbooking.security.UserPrincipal currentUser = authorizationService.getCurrentUser();
        if (!caseEntity.getUserId().equals(currentUser.getUserId())) {
            throw new UnauthorizedException("You can only update your own cases");
        }
        
        // Only allow editing DRAFT cases
        if (caseEntity.getCaseStatus() != CaseStatus.DRAFT) {
            throw new BadRequestException("Only DRAFT cases can be edited. Current status: " + caseEntity.getCaseStatus());
        }
        
        // Update fields if provided
        if (request.getCaseTitle() != null && !request.getCaseTitle().trim().isEmpty()) {
            caseEntity.setCaseTitle(request.getCaseTitle());
        }
        if (request.getDescription() != null && !request.getDescription().trim().isEmpty()) {
            String originalDesc = request.getDescription();
            // Automatically mask PII in the new description
            try {
                logger.info("Masking PII in updated description for case {}", caseId);
                String maskedDesc = textMaskingService.maskEnglishPersonalInfo(originalDesc);
                caseEntity.setDescription(maskedDesc);
            } catch (Exception e) {
                logger.error("Failed to mask description update for case {}. Using original text.", caseId, e);
                caseEntity.setDescription(originalDesc);
            }
        }
        if (request.getCaseType() != null) {
            caseEntity.setCaseType(request.getCaseType());
        }
        
        Case saved = caseRepository.save(caseEntity);
        CaseDTO dto = convertToDTO(saved);
        
        // Audit Log
        try {
            auditLogService.logEvent(
                caseId,
                "CASE_UPDATED",
                null,
                caseEntity.getCaseStatus().name(),
                "Draft case updated",
                currentUser.getUserId(),
                currentUser.getRole()
            );
        } catch (Exception e) {
            logger.warn("Could not log audit for case update: {}", e.getMessage());
        }
        
        return dto;
    }

    public CaseDTO getCaseById(Long id) {

        Case caseEntity = caseRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found with id: " + id));
        return convertToDTO(caseEntity);
    }
    public List<CaseDTO> getAllCasesForAdmin() {
        return caseRepository.findAllByDeletedFalse().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<CaseDTO> getCasesForUser(Long userId) {
        return caseRepository.findByUserIdAndDeletedFalse(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<CaseDTO> getCasesForLawyer(Long lawyerId) {
        // This method returns ONLY cases assigned to this lawyer
        // For unassigned/recommended cases, use getRecommendedCases()
        return caseRepository.findByLawyerIdAndDeletedFalse(lawyerId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<CaseDTO> getRecommendedCases(Long lawyerId) {
        var lawyer = lawyerRepository.findById(lawyerId)
                .orElseThrow(() -> new ResourceNotFoundException("Lawyer not found"));
        
        java.util.Set<CaseType> specs = lawyer.getSpecializations();
        if (specs == null || specs.isEmpty()) {
            // No specializations = no recommendations, only show unassigned PUBLISHED cases
            return caseRepository.findUnassigned().stream()
                    .filter(c -> c.getCaseStatus() == CaseStatus.PUBLISHED || 
                                 c.getCaseStatus() == CaseStatus.UNDER_REVIEW ||
                                 c.getCaseStatus() == CaseStatus.PENDING_APPROVAL ||
                                 c.getCaseStatus() == CaseStatus.IN_PROGRESS)
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        }
        
        // Return unassigned PUBLISHED cases that match this lawyer's specializations
        return caseRepository.findUnassignedBySpecializations(specs).stream()
                .filter(c -> c.getCaseStatus() == CaseStatus.PUBLISHED || 
                             c.getCaseStatus() == CaseStatus.UNDER_REVIEW ||
                             c.getCaseStatus() == CaseStatus.PENDING_APPROVAL ||
                             c.getCaseStatus() == CaseStatus.IN_PROGRESS)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @org.springframework.transaction.annotation.Transactional
    public CaseDTO assignLawyerToCase(Long caseId, Long lawyerId) {
        Case caseEntity = caseRepository.findById(caseId)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found with id: " + caseId));
            
        var lawyer = lawyerRepository.findById(lawyerId)
            .orElseThrow(() -> new ResourceNotFoundException("Lawyer not found"));

        // ... (rest of method remains same, but using findById)
        // [Truncating for brevity, only replacing the initial part]
        // Actually I need to be careful with the truncated part.
        // Let's do a more targeted replace.

        // Validation: Prevent re-assignment if already assigned
        if (caseEntity.getLawyerId() != null) {
            if (caseEntity.getLawyerId().equals(lawyerId)) {
                return convertToDTO(caseEntity); // Already assigned to this lawyer
            }
            logger.warn("Lawyer {} attempted to claim case {} which is already assigned to lawyer {}", 
                        lawyerId, caseId, caseEntity.getLawyerId());
            throw new BadRequestException("This case has already been accepted by another lawyer");
        }

        // Validation: Ensure lawyer specialization matches case category (if both exist)
        CaseType type = caseEntity.getCaseType();
        java.util.Set<CaseType> specs = lawyer.getSpecializations();
        
        if (type != null && specs != null && !specs.contains(type)) {
            logger.warn("Lawyer {} (specs: {}) does not match case type: {}", 
                        lawyerId, specs, type);
            throw new BadRequestException("You can only accept cases that match your specializations. This case is categorized as: " + type);
        }

        caseEntity.setLawyerId(lawyerId);
        CaseStatus oldStatus = caseEntity.getCaseStatus();
        
        // Determine status based on who is assigning
        com.legalconnect.lawyerbooking.security.UserPrincipal currentUser = authorizationService.getCurrentUser();
        CaseStatus newStatus;
        String eventType;
        
        if (currentUser.getRole().equalsIgnoreCase("user")) {
            newStatus = CaseStatus.PENDING_APPROVAL;
            eventType = "USER_ASSIGNED_LAWYER";
        } else {
            // Disable lawyer self-claiming (direct connection) to enforce the bidding/payment flow
            throw new BadRequestException("Self-claiming cases is no longer supported. Please submit an offer instead to connect with the client.");
        }

        caseEntity.setCaseStatus(newStatus);
        Case updated = caseRepository.save(caseEntity);
        CaseDTO dto = convertToDTO(updated);

        // Audit Log
        try {
            auditLogService.logEvent(
                caseId,
                eventType,
                oldStatus.name(),
                newStatus.name(),
                (newStatus == CaseStatus.PENDING_APPROVAL ? "Invitation sent to: " : "Assigned to: ") + lawyer.getFullName(),
                currentUser.getUserId(),
                currentUser.getRole()
            );
        } catch (Exception e) {
            logger.warn("Could not log audit for lawyer assignment: {}", e.getMessage());
        }

        // SYNC: Update any linked ClientAudio record
        try {
            java.util.List<com.legalconnect.lawyerbooking.entity.ClientAudio> audios = 
                clientAudioRepository.findByCaseId(caseId);
            for (com.legalconnect.lawyerbooking.entity.ClientAudio audio : audios) {
                audio.setLawyerId(lawyerId);
                clientAudioRepository.save(audio);
            }
        } catch (Exception e) {
            logger.error("Failed to sync ClientAudio with case assignment: {}", e.getMessage());
        }

        // Broadcast that a case has been taken
        try {
            messagingTemplate.convertAndSend("/topic/lawyer/updates", Map.of(
                "type", "CASE_ASSIGNED",
                "caseId", caseId,
                "lawyerId", lawyerId
            ));

            // Notify Lawyer via Email
            if (lawyer.getEmail() != null) {
                notificationService.notifyLawyerOfNewCase(lawyer.getEmail(), caseId, caseEntity.getCaseTitle());
            }

        } catch (Exception e) {
            logger.error("Failed to broadcast/notify case assignment: {}", e.getMessage());
        }

        return dto;
    }

    public CaseDTO updateCaseSolution(Long caseId, String solution) {
        Case caseEntity = caseRepository.findById(caseId)
            .orElseThrow(() -> new RuntimeException("Case not found with id: " + caseId));
        caseEntity.setSolution(solution);
        Case updated = caseRepository.save(caseEntity);
        CaseDTO dto = convertToDTO(updated);
        
        messagingTemplate.convertAndSend("/topic/case/" + caseId, dto);
        
        return dto;
    }

    public CaseDTO updateCaseStatus(Long caseId, String status) {
        if (status == null) {
             throw new BadRequestException("Status cannot be null");
        }

        CaseStatus newStatus;
        try {
            String normStatus = status.trim().toUpperCase().replace(" ", "_");
            newStatus = CaseStatus.valueOf(normStatus);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid case status received: '{}'", status);
            throw new BadRequestException("Invalid case status: " + status + ". Expected values: OPEN, IN_PROGRESS, CLOSED, etc.");
        }

        Case caseEntity = caseRepository.findById(caseId)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found with id: " + caseId));
        
        logger.info("Updating case {} status from {} to {}", caseId, caseEntity.getCaseStatus(), newStatus);
        CaseStatus oldStatus = caseEntity.getCaseStatus();
        caseEntity.setCaseStatus(newStatus);
        
        // Phase 27: Automated Stats
        if (newStatus == CaseStatus.CLOSED && oldStatus != CaseStatus.CLOSED && caseEntity.getLawyerId() != null) {
            try {
                com.legalconnect.lawyerbooking.entity.Lawyer lawyer = lawyerRepository.findById(caseEntity.getLawyerId()).orElse(null);
                if (lawyer != null) {
                    Integer currentCount = lawyer.getCompletedCasesCount();
                    lawyer.setCompletedCasesCount(currentCount == null ? 1 : currentCount + 1);
                    lawyerRepository.save(lawyer);
                    logger.info("Incremented completedCasesCount for lawyer {}", lawyer.getId());
                }
            } catch (Exception e) {
                logger.error("Failed to update lawyer stats after case closure: {}", e.getMessage());
            }
        }

        Case updated = caseRepository.save(caseEntity);
        CaseDTO dto = convertToDTO(updated);

        // Audit Log
        try {
            com.legalconnect.lawyerbooking.security.UserPrincipal currentUser = authorizationService.getCurrentUser();
            auditLogService.logEvent(
                caseId,
                "STATUS_CHANGE",
                oldStatus != null ? oldStatus.name() : null,
                newStatus.name(),
                "Status updated to " + newStatus.name(),
                currentUser.getUserId(),
                currentUser.getRole()
            );
        } catch (Exception e) {
            logger.warn("Could not log audit for status change: {}", e.getMessage());
        }
        
        messagingTemplate.convertAndSend("/topic/case/" + caseId, dto);
        
        return dto;
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteCase(Long caseId) {
        Case caseEntity = caseRepository.findById(caseId)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found with id: " + caseId));
        
        // SOFT DELETE logic for history and security
        caseEntity.setDeleted(true);
        caseRepository.save(caseEntity);
        logger.info("Case {} soft-deleted", caseId);

        try {
            messagingTemplate.convertAndSend("/topic/lawyer/updates", Map.of(
                "type", "CASE_DELETED",
                "caseId", caseId
            ));
        } catch (Exception e) {
            logger.error("Failed to broadcast case deletion: {}", e.getMessage());
        }
    }

    @Transactional
    public CaseDTO acceptCaseRequest(Long caseId) {
        throw new BadRequestException("Direct acceptance is no longer supported. Please submit a proposal to the client to initiate payment and start the case.");
    }

    @Transactional
    public CaseDTO declineCaseRequest(Long caseId) {
        Case caseEntity = caseRepository.findById(caseId)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found"));
            
        if (caseEntity.getCaseStatus() != CaseStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Case is not in pending approval state");
        }

        com.legalconnect.lawyerbooking.security.UserPrincipal currentUser = authorizationService.getCurrentUser();
        if (!caseEntity.getLawyerId().equals(currentUser.getUserId())) {
            throw new UnauthorizedException("Only the assigned lawyer can decline this request");
        }

        CaseStatus oldStatus = caseEntity.getCaseStatus();
        caseEntity.setLawyerId(null);
        caseEntity.setCaseStatus(CaseStatus.PUBLISHED);
        Case saved = caseRepository.save(caseEntity);

        auditLogService.logEvent(
            caseId,
            "CASE_DECLINED",
            oldStatus.name(),
            CaseStatus.OPEN.name(),
            "Lawyer declined the case assignment",
            currentUser.getUserId(),
            currentUser.getRole()
        );

        return convertToDTO(saved);
    }

    public CaseDTO convertToDTO(Case caseEntity) {
        CaseDTO dto = new CaseDTO(
            caseEntity.getId(),
            caseEntity.getUserId(),
            caseEntity.getLawyerId(),
            caseEntity.getCaseTitle(),
            caseEntity.getCaseType(),
            caseEntity.getCaseStatus(),
            caseEntity.getDescription(),
            caseEntity.getSolution(),
            caseEntity.getCreatedAt(),
            caseEntity.getUpdatedAt()
        );
        
        // Populate user full name
        if (caseEntity.getUserId() != null) {
            userRepository.findById(caseEntity.getUserId()).ifPresent(user -> {
                dto.setUserFullName(user.getFullName());
                dto.setUserEmail(user.getEmail());
            });
        }
        
        // Populate lawyer full name
        if (caseEntity.getLawyerId() != null) {
            lawyerRepository.findById(caseEntity.getLawyerId()).ifPresent(lawyer -> {
                dto.setLawyerFullName(lawyer.getFullName());
            });
        }
        
        return dto;
    }
}

