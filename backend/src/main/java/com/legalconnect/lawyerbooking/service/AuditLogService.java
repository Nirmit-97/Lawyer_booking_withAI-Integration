package com.legalconnect.lawyerbooking.service;

import com.legalconnect.lawyerbooking.entity.AuditLog;
import com.legalconnect.lawyerbooking.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    /**
     * Log an admin action
     */
    public void logAction(Long adminId, String adminName, String action, String targetType, Long targetId, String details) {
        AuditLog log = new AuditLog(adminId, adminName, action, targetType, targetId, details);
        auditLogRepository.save(log);
    }

    /**
     * Get all audit logs with pagination
     */
    public Page<AuditLog> getAuditLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        return auditLogRepository.findAll(pageable);
    }

    /**
     * Get audit logs filtered by date range
     */
    public Page<AuditLog> getAuditLogsByDateRange(LocalDateTime start, LocalDateTime end, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        return auditLogRepository.findByTimestampBetween(start, end, pageable);
    }

    /**
     * Get audit logs for a specific admin
     */
    public Page<AuditLog> getAuditLogsByAdmin(Long adminId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        return auditLogRepository.findByAdminId(adminId, pageable);
    }

    /**
     * Get audit logs for a specific target
     */
    public Page<AuditLog> getAuditLogsForTarget(String targetType, Long targetId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        return auditLogRepository.findByTargetTypeAndTargetId(targetType, targetId, pageable);
    }
}
