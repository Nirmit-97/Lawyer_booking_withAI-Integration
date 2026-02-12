package com.legalconnect.lawyerbooking.repository;

import com.legalconnect.lawyerbooking.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    Page<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    Page<AuditLog> findByAdminId(Long adminId, Pageable pageable);
    
    Page<AuditLog> findByTargetTypeAndTargetId(String targetType, Long targetId, Pageable pageable);
}
