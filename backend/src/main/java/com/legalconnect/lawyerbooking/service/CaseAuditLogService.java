package com.legalconnect.lawyerbooking.service;

import com.legalconnect.lawyerbooking.dto.CaseAuditLogDTO;
import com.legalconnect.lawyerbooking.entity.CaseAuditLog;
import com.legalconnect.lawyerbooking.repository.CaseAuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CaseAuditLogService {

    @Autowired
    private CaseAuditLogRepository auditLogRepository;

    @Transactional
    public void logEvent(Long caseId, String eventType, String oldStatus, String newStatus, String message, Long actorId, String actorRole) {
        CaseAuditLog log = new CaseAuditLog();
        log.setCaseId(caseId);
        log.setEventType(eventType);
        log.setOldStatus(oldStatus);
        log.setNewStatus(newStatus);
        log.setMessage(message);
        log.setTriggeredById(actorId);
        log.setTriggeredByRole(actorRole);
        auditLogRepository.save(log);
    }

    public List<CaseAuditLogDTO> getTimelineForCase(Long caseId) {
        return auditLogRepository.findByCaseIdOrderByCreatedAtDesc(caseId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private CaseAuditLogDTO convertToDTO(CaseAuditLog log) {
        CaseAuditLogDTO dto = new CaseAuditLogDTO();
        dto.setId(log.getId());
        dto.setCaseId(log.getCaseId());
        dto.setEventType(log.getEventType());
        dto.setOldStatus(log.getOldStatus());
        dto.setNewStatus(log.getNewStatus());
        dto.setMessage(log.getMessage());
        dto.setTriggeredById(log.getTriggeredById());
        dto.setTriggeredByRole(log.getTriggeredByRole());
        dto.setCreatedAt(log.getCreatedAt());
        return dto;
    }
}
