package com.legalconnect.lawyerbooking.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import com.legalconnect.lawyerbooking.service.CaseService;
import com.legalconnect.lawyerbooking.service.AuthorizationService;
import com.legalconnect.lawyerbooking.util.JwtUtil;
import java.util.stream.Collectors;
import com.legalconnect.lawyerbooking.dto.CaseDTO;
import com.legalconnect.lawyerbooking.dto.CaseRequest;
import com.legalconnect.lawyerbooking.exception.UnauthorizedException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cases")
@CrossOrigin(origins = "*")
public class CaseController {

    private static final Logger logger = LoggerFactory.getLogger(CaseController.class);

    @Autowired
    private CaseService caseService;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private com.legalconnect.lawyerbooking.repository.LawyerRepository lawyerRepository;

    @PostMapping
    public ResponseEntity<CaseDTO> createCase(@RequestBody CaseRequest request) {
        try {
            logger.info("Creating new case for user: {}", request.getUserId());
            CaseDTO created = caseService.createCase(request);
            logger.info("Case created successfully with ID: {}", created.getId());
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            logger.error("Error creating case: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PutMapping("/{caseId}/publish")
    public ResponseEntity<CaseDTO> publishCase(@PathVariable("caseId") Long caseId) {
        try {
            logger.info("Publishing case ID: {}", caseId);
            CaseDTO published = caseService.publishCase(caseId);
            logger.info("Case {} published successfully", caseId);
            return ResponseEntity.ok(published);
        } catch (Exception e) {
            logger.error("Error publishing case {}: {}", caseId, e.getMessage(), e);
            throw e;
        }
    }

    @PutMapping("/{caseId}")
    public ResponseEntity<CaseDTO> updateCase(
            @PathVariable("caseId") Long caseId,
            @RequestBody CaseRequest request) {
        try {
            logger.info("Updating case ID: {}", caseId);
            CaseDTO updated = caseService.updateCase(caseId, request);
            logger.info("Case {} updated successfully", caseId);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            logger.error("Error updating case {}: {}", caseId, e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<CaseDTO> getCaseById(@PathVariable("id") Long id) {
        logger.info("Fetching details for case ID: {}", id);
        try {
            authorizationService.verifyCaseAccess(id);
            CaseDTO caseDTO = caseService.getCaseById(id);
            return ResponseEntity.ok(caseDTO);
        } catch (UnauthorizedException e) {
            logger.warn("Access denied to case {}: {}", id, e.getMessage());
            return ResponseEntity.status(403).body(null);
        } catch (com.legalconnect.lawyerbooking.exception.ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error fetching case {}", id, e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CaseDTO>> getCasesByUserId(@PathVariable("userId") Long userId) {
        try {
            // Verify that the requester is the user with this ID
            com.legalconnect.lawyerbooking.security.UserPrincipal currentUser = authorizationService.getCurrentUser();
            
            logger.info("DEBUG_AUTH: userId={}, currentUserId={}, currentUserType={}", 
                userId, currentUser.getUserId(), currentUser.getRole());

            if (!currentUser.getRole().equalsIgnoreCase("user") || !currentUser.getUserId().equals(userId)) {
                logger.warn("Unauthorized access to cases for user {}. Principal: {}", userId, currentUser.getName());
                return ResponseEntity.status(401).build();
            }
            
            List<CaseDTO> cases = caseService.getCasesForUser(userId);
            return ResponseEntity.ok(cases);
        } catch (UnauthorizedException e) {
            logger.warn("Unauthorized in getCasesByUserId: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        }
    }

    @GetMapping("/lawyer/{lawyerId}")
    public ResponseEntity<List<CaseDTO>> getCasesByLawyerId(@PathVariable("lawyerId") Long lawyerId) {
        try {
            authorizationService.verifyLawyerAccess(lawyerId);
            List<CaseDTO> cases = caseService.getCasesForLawyer(lawyerId);
            return ResponseEntity.ok(cases);
        } catch (UnauthorizedException e) {
            logger.warn("Unauthorized in getCasesByLawyerId: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        } catch (Exception e) {
            logger.error("Error in getCasesByLawyerId: ", e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/unassigned")
    public ResponseEntity<List<CaseDTO>> getUnassignedCases() {
        try {
            com.legalconnect.lawyerbooking.security.UserPrincipal currentUser = authorizationService.getCurrentUser();
            
            if ("lawyer".equalsIgnoreCase(currentUser.getRole())) {
                // For lawyers, return cases that match their specializations and are unassigned
                return ResponseEntity.ok(caseService.getRecommendedCases(currentUser.getUserId()));
            }
            
            // For Admin or non-lawyers, return all unassigned cases via service
            return ResponseEntity.ok(caseService.getAllCasesForAdmin().stream()
                .filter(c -> c.getLawyerId() == null)
                .collect(Collectors.toList()));
        } catch (Exception e) {
            logger.error("Error in getUnassignedCases: {}", e.getMessage());
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }
    }

    @GetMapping("/recommended/{lawyerId}")
    public ResponseEntity<List<CaseDTO>> getRecommendedCases(@PathVariable Long lawyerId) {
        try {
            authorizationService.verifyLawyerAccess(lawyerId);
            List<CaseDTO> cases = caseService.getRecommendedCases(lawyerId);
            return ResponseEntity.ok(cases);
        } catch (UnauthorizedException e) {
            logger.warn("Unauthorized in getRecommendedCases: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/{caseId}/assign")
    public ResponseEntity<CaseDTO> assignLawyerToCase(
            @PathVariable("caseId") Long caseId,
            @RequestBody Map<String, Long> request) {
        try {
            Long lawyerId = request.get("lawyerId");
            authorizationService.verifyCaseAssignmentAction(caseId, lawyerId);
            CaseDTO caseDTO = caseService.assignLawyerToCase(caseId, lawyerId);
            return ResponseEntity.ok(caseDTO);
        } catch (UnauthorizedException e) {
            logger.warn("Unauthorized in assignLawyerToCase: {}", e.getMessage());
            return ResponseEntity.status(401).build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{caseId}/accept")
    public ResponseEntity<CaseDTO> acceptCase(@PathVariable("caseId") Long caseId) {
        try {
            CaseDTO caseDTO = caseService.acceptCaseRequest(caseId);
            return ResponseEntity.ok(caseDTO);
        } catch (com.legalconnect.lawyerbooking.exception.UnauthorizedException e) {
            return ResponseEntity.status(401).build();
        } catch (com.legalconnect.lawyerbooking.exception.BadRequestException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/{caseId}/decline")
    public ResponseEntity<CaseDTO> declineCase(@PathVariable("caseId") Long caseId) {
        try {
            CaseDTO caseDTO = caseService.declineCaseRequest(caseId);
            return ResponseEntity.ok(caseDTO);
        } catch (com.legalconnect.lawyerbooking.exception.UnauthorizedException e) {
            return ResponseEntity.status(401).build();
        } catch (com.legalconnect.lawyerbooking.exception.BadRequestException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @PutMapping("/{caseId}/solution")
    public ResponseEntity<CaseDTO> updateCaseSolution(
            @PathVariable("caseId") Long caseId,
            @RequestBody Map<String, String> request) {
        try {
            authorizationService.verifyCaseUpdateAccess(caseId);
            
            // Explicitly require lawyer role for solution updates
            if (!authorizationService.getCurrentUser().getRole().equalsIgnoreCase("lawyer")) {
                logger.warn("User {} attempted to update solution for case {}", 
                           authorizationService.getCurrentUser().getUserId(), caseId);
                return ResponseEntity.status(403).build();
            }
            
            String solution = request.get("solution");
            CaseDTO caseDTO = caseService.updateCaseSolution(caseId, solution);
            return ResponseEntity.ok(caseDTO);
        } catch (UnauthorizedException e) {
            logger.warn("Unauthorized attempt to update case solution {}", caseId);
            return ResponseEntity.status(401).build();
        } catch (Exception e) {
            logger.error("Error updating case solution {}", caseId, e);
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{caseId}/status")
    public ResponseEntity<CaseDTO> updateCaseStatus(
            @PathVariable("caseId") Long caseId,
            @RequestBody Map<String, String> request) {
        logger.info("Received status update request for case ID: {}", caseId);
        try {
            authorizationService.verifyCaseUpdateAccess(caseId);
            
            String status = request.get("status");
            if (status == null || status.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            CaseDTO caseDTO = caseService.updateCaseStatus(caseId, status);
            return ResponseEntity.ok(caseDTO);
        } catch (UnauthorizedException e) {
            logger.warn("Access denied for status update of case {}: {}", caseId, e.getMessage());
            return ResponseEntity.status(403).build();
        } catch (com.legalconnect.lawyerbooking.exception.ResourceNotFoundException e) {
            logger.warn("Case not found for status update: {}", caseId);
            return ResponseEntity.notFound().build(); 
        } catch (com.legalconnect.lawyerbooking.exception.BadRequestException e) {
            logger.warn("Bad request for status update of case {}: {}", caseId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error updating case status {}: {}", caseId, e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }
}

