package com.legalconnect.lawyerbooking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.legalconnect.lawyerbooking.entity.Case;
import com.legalconnect.lawyerbooking.enums.CaseStatus;
import com.legalconnect.lawyerbooking.enums.CaseType;
import com.legalconnect.lawyerbooking.exception.BadRequestException;
import com.legalconnect.lawyerbooking.exception.ResourceNotFoundException;
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
    private CaseClassificationService classificationService;

    @Autowired
    private ClientAudioRepository clientAudioRepository;

    @Autowired
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

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
        caseEntity.setCaseStatus(CaseStatus.OPEN);
        caseEntity.setDeleted(false);
        
        Case saved = caseRepository.save(caseEntity);
        CaseDTO dto = convertToDTO(saved);
        
        // Broadcast new case to lawyers
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
            // No specializations = no recommendations, only show unassigned cases
            return caseRepository.findUnassigned().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        }
        
        // Return unassigned cases that match this lawyer's specializations
        return caseRepository.findForLawyer(lawyerId, specs).stream()
                .filter(c -> c.getLawyerId() == null) // Only unassigned
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
        caseEntity.setCaseStatus(CaseStatus.IN_PROGRESS);
        Case updated = caseRepository.save(caseEntity);
        CaseDTO dto = convertToDTO(updated);

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
        } catch (Exception e) {
            logger.error("Failed to broadcast case assignment: {}", e.getMessage());
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
        caseEntity.setCaseStatus(newStatus);
        Case updated = caseRepository.save(caseEntity);
        CaseDTO dto = convertToDTO(updated);
        
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

    public CaseDTO convertToDTO(Case caseEntity) {
        return new CaseDTO(
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
    }
}

