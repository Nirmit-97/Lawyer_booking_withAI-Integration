package com.legalconnect.lawyerbooking.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "case_audit_logs")
public class CaseAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @Column(name = "event_type", nullable = false)
    private String eventType; // STATUS_CHANGE, DOCUMENT_UPLOAD

    @Column(name = "old_status")
    private String oldStatus;

    @Column(name = "new_status")
    private String newStatus;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "triggered_by_id")
    private Long triggeredById;

    @Column(name = "triggered_by_role")
    private String triggeredByRole;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public CaseAuditLog() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCaseId() { return caseId; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getOldStatus() { return oldStatus; }
    public void setOldStatus(String oldStatus) { this.oldStatus = oldStatus; }

    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getTriggeredById() { return triggeredById; }
    public void setTriggeredById(Long triggeredById) { this.triggeredById = triggeredById; }

    public String getTriggeredByRole() { return triggeredByRole; }
    public void setTriggeredByRole(String triggeredByRole) { this.triggeredByRole = triggeredByRole; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
