package com.legalconnect.lawyerbooking.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public class CreateOfferRequest {
    
    @NotNull(message = "Proposed fee is required")
    @Positive(message = "Proposed fee must be positive")
    private BigDecimal proposedFee;
    
    private String estimatedTimeline;
    
    private String proposalMessage;
    
    private String consultationType; // e.g., "IN_PERSON", "VIRTUAL", "HYBRID"
    
    private String milestonePlan; // Optional JSON string for Phase 2

    // Constructors
    public CreateOfferRequest() {}

    // Getters and Setters
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
}
