package com.legalconnect.lawyerbooking.dto;

import java.time.LocalDateTime;

public class CaseAuditLogDTO {
    private Long id;
    private Long caseId;
    private String eventType;
    private String oldStatus;
    private String newStatus;
    private String message;
    private Long triggeredById;
    private String triggeredByRole;
    private LocalDateTime createdAt;

    // Getters and Setters
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
