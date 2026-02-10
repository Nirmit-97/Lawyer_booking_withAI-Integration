package com.legalconnect.lawyerbooking.dto;

import com.legalconnect.lawyerbooking.enums.CaseStatus;
import com.legalconnect.lawyerbooking.enums.CaseType;
import java.time.LocalDateTime;

public class CaseDTO {
    private Long id;
    private Long userId;
    private Long lawyerId;
    private String caseTitle;
    private CaseType caseType;
    private CaseStatus caseStatus;
    private String description;
    private String solution;
    private Double lawyerFee;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public CaseDTO() {}

    public CaseDTO(Long id, Long userId, Long lawyerId, String caseTitle,
                   CaseType caseType, CaseStatus caseStatus, String description, 
                   String solution, Double lawyerFee, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.lawyerId = lawyerId;
        this.caseTitle = caseTitle;
        this.caseType = caseType;
        this.caseStatus = caseStatus;
        this.description = description;
        this.solution = solution;
        this.lawyerFee = lawyerFee;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getLawyerId() {
        return lawyerId;
    }

    public void setLawyerId(Long lawyerId) {
        this.lawyerId = lawyerId;
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

    // Alias for frontend compatibility (CaseList.js expects caseCategory)
    public String getCaseCategory() {
        return caseType != null ? caseType.name() : null;
    }

    public void setCaseCategory(String category) {
        if (category != null) {
            try {
                this.caseType = CaseType.valueOf(category.trim().toUpperCase().replace(" ", "_"));
            } catch (Exception e) {
                this.caseType = CaseType.OTHER;
            }
        }
    }

    public CaseStatus getCaseStatus() {
        return caseStatus;
    }

    public void setCaseStatus(CaseStatus caseStatus) {
        this.caseStatus = caseStatus;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSolution() {
        return solution;
    }

    public void setSolution(String solution) {
        this.solution = solution;
    }

    public Double getLawyerFee() {
        return lawyerFee;
    }

    public void setLawyerFee(Double lawyerFee) {
        this.lawyerFee = lawyerFee;
    }


    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

