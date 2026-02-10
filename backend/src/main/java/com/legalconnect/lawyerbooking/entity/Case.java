package com.legalconnect.lawyerbooking.entity;

import com.legalconnect.lawyerbooking.enums.CaseStatus;
import com.legalconnect.lawyerbooking.enums.CaseType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cases")
public class Case {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "lawyer_id")
    private Long lawyerId;

    @Column(name = "lawyer_fee")
    private Double lawyerFee;

    @Column(name = "case_title", nullable = false, length = 255)
    private String caseTitle;

    @Convert(converter = com.legalconnect.lawyerbooking.converter.CaseTypeConverter.class)
    @Column(name = "case_type", length = 50)
    private CaseType caseType;

    @Column(name = "case_status", length = 50)
    @Convert(converter = com.legalconnect.lawyerbooking.converter.CaseStatusConverter.class)
    private CaseStatus caseStatus = CaseStatus.OPEN; 

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Lob
    @Column(name = "description", columnDefinition = "LONGTEXT")
    private String description;

    @Column(name = "solution", columnDefinition = "LONGTEXT")
    private String solution; // Solution provided by lawyer

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (caseStatus == null) {
            caseStatus = CaseStatus.OPEN;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
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

    public Double getLawyerFee() {
        return lawyerFee;
    }

    public void setLawyerFee(Double lawyerFee) {
        this.lawyerFee = lawyerFee;
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

    public CaseStatus getCaseStatus() {
        return caseStatus;
    }

    public void setCaseStatus(CaseStatus caseStatus) {
        this.caseStatus = caseStatus;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
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

