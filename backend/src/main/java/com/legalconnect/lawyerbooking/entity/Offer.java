package com.legalconnect.lawyerbooking.entity;

import com.legalconnect.lawyerbooking.enums.OfferStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "offers", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"case_id", "lawyer_id"}))
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @Column(name = "lawyer_id", nullable = false)
    private Long lawyerId;

    @Column(name = "proposed_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal proposedFee;

    @Column(name = "estimated_timeline", length = 100)
    private String estimatedTimeline;

    @Lob
    @Column(name = "proposal_message", columnDefinition = "TEXT")
    private String proposalMessage;

    @Column(name = "consultation_type", length = 50)
    private String consultationType;

    @Lob
    @Column(name = "milestone_plan", columnDefinition = "TEXT")
    private String milestonePlan; // JSON string for Phase 2

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OfferStatus status = OfferStatus.SUBMITTED;

    @Column(name = "viewed_by_user", nullable = false)
    private boolean viewedByUser = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (expiresAt == null) {
            expiresAt = createdAt.plusHours(48); // 48 hours from creation
        }
        if (status == null) {
            status = OfferStatus.SUBMITTED;
        }
    }

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

    public LocalDateTime getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(LocalDateTime rejectedAt) {
        this.rejectedAt = rejectedAt;
    }
}
