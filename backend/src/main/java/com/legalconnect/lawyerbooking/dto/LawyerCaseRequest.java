package com.legalconnect.lawyerbooking.dto;

import com.legalconnect.lawyerbooking.enums.CaseType;
import java.io.Serializable;

public class LawyerCaseRequest implements Serializable {
    private Long caseId;
    private String title;
    private CaseType caseType;
    private String description;
    private Long userId;
    private String createdAt;

    public LawyerCaseRequest() {}

    public LawyerCaseRequest(Long caseId, String title, CaseType caseType, String description, Long userId) {
        this.caseId = caseId;
        this.title = title;
        this.caseType = caseType;
        this.description = description;
        this.userId = userId;
        this.createdAt = java.time.LocalDateTime.now().toString();
    }

    public Long getCaseId() { return caseId; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public CaseType getCaseType() { return caseType; }
    public void setCaseType(CaseType caseType) { this.caseType = caseType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
