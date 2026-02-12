package com.legalconnect.lawyerbooking.dto;

import com.legalconnect.lawyerbooking.enums.OfferStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OfferDTO {
    private Long id;
    private Long caseId;
    private Long lawyerId;
    private String lawyerName;
    private String lawyerSpecialization;
    private Integer lawyerExperience;
    private Double lawyerRating;
    private BigDecimal proposedFee;
    private String estimatedTimeline;
    private String proposalMessage;
    private String consultationType;
    private String milestonePlan;
    private OfferStatus status;
    private boolean viewedByUser;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime acceptedAt;

    // Constructors
    public OfferDTO() {}

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }

    public Long getLawyerId() {
        return lawyerId;
    }

    public void setLawyerId(Long lawyerId) {
        this.lawyerId = lawyerId;
    }

    public String getLawyerName() {
        return lawyerName;
    }

    public void setLawyerName(String lawyerName) {
        this.lawyerName = lawyerName;
    }

    public String getLawyerSpecialization() {
        return lawyerSpecialization;
    }

    public void setLawyerSpecialization(String lawyerSpecialization) {
        this.lawyerSpecialization = lawyerSpecialization;
    }

    public Integer getLawyerExperience() {
        return lawyerExperience;
    }

    public void setLawyerExperience(Integer lawyerExperience) {
        this.lawyerExperience = lawyerExperience;
    }

    public Double getLawyerRating() {
        return lawyerRating;
    }

    public void setLawyerRating(Double lawyerRating) {
        this.lawyerRating = lawyerRating;
    }

    public BigDecimal getProposedFee() {
        return proposedFee;
    }

    public void setProposedFee(BigDecimal proposedFee) {
        this.proposedFee = proposedFee;
    }

    public String getEstimatedTimeline() {
        return estimatedTimeline;
    }

    public void setEstimatedTimeline(String estimatedTimeline) {
        this.estimatedTimeline = estimatedTimeline;
    }

    public String getProposalMessage() {
        return proposalMessage;
    }

    public void setProposalMessage(String proposalMessage) {
        this.proposalMessage = proposalMessage;
    }

    public String getConsultationType() {
        return consultationType;
    }

    public void setConsultationType(String consultationType) {
        this.consultationType = consultationType;
    }

    public String getMilestonePlan() {
        return milestonePlan;
    }

    public void setMilestonePlan(String milestonePlan) {
        this.milestonePlan = milestonePlan;
    }

    public OfferStatus getStatus() {
        return status;
    }

    public void setStatus(OfferStatus status) {
        this.status = status;
    }

    public boolean isViewedByUser() {
        return viewedByUser;
    }

    public void setViewedByUser(boolean viewedByUser) {
        this.viewedByUser = viewedByUser;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }
}
