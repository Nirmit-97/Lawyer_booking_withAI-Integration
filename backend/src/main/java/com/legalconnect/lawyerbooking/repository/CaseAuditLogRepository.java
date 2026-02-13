package com.legalconnect.lawyerbooking.repository;

import com.legalconnect.lawyerbooking.entity.CaseAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaseAuditLogRepository extends JpaRepository<CaseAuditLog, Long> {
    List<CaseAuditLog> findByCaseIdOrderByCreatedAtDesc(Long caseId);
}
