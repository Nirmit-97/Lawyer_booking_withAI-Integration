package com.legalconnect.lawyerbooking.dto;

import com.legalconnect.lawyerbooking.enums.CaseType;

public class CaseRequest {
    private Long userId;
    private String caseTitle;
    private CaseType caseType;
    private String description;
    private Long lawyerId;

    // Constructors
    public CaseRequest() {}

    public CaseRequest(Long userId, String caseTitle, CaseType caseType, String description) {
        this.userId = userId;
        this.caseTitle = caseTitle;
        this.caseType = caseType;
        this.description = description;
    }

    public CaseRequest(Long userId, String caseTitle, CaseType caseType, String description, Long lawyerId) {
        this.userId = userId;
        this.caseTitle = caseTitle;
        this.caseType = caseType;
        this.description = description;
        this.lawyerId = lawyerId;
    }

    // Getters and Setters
    public Long getLawyerId() {
        return lawyerId;
    }

    public void setLawyerId(Long lawyerId) {
        this.lawyerId = lawyerId;
    }
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCaseTitle() {
        return caseTitle;
    }

    public void setCaseTitle(String caseTitle) {
        this.caseTitle = caseTitle;
    }

    public CaseType getCaseType() {
        return caseType;
    }

    public void setCaseType(CaseType caseType) {
        this.caseType = caseType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

