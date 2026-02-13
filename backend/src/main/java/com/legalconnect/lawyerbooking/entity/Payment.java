package com.legalconnect.lawyerbooking.entity;

import com.legalconnect.lawyerbooking.enums.PaymentStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @Column(name = "offer_id", nullable = false)
    private Long offerId;

    @Column(name = "lawyer_id", nullable = false)
    private Long lawyerId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Fee breakdown
    @Column(name = "lawyer_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal lawyerFee; // What lawyer proposed (e.g., ₹10,000)

    @Column(name = "platform_fee", precision = 10, scale = 2)
    private BigDecimal platformFee; // 10% of lawyerFee (e.g., ₹1,000)

    @Column(name = "gateway_fee", precision = 10, scale = 2)
    private BigDecimal gatewayFee; // ~2% Razorpay charges (e.g., ₹200)

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount; // lawyerFee + gatewayFee (what user pays)

    @Column(name = "lawyer_payout", precision = 10, scale = 2)
    private BigDecimal lawyerPayout; // lawyerFee - platformFee (what lawyer receives)

    // Gateway details
    @Column(name = "gateway", length = 50)
    private String gateway = "razorpay";

    @Column(name = "gateway_order_id", unique = true)
    private String gatewayOrderId;

    @Column(name = "gateway_transaction_id", unique = true)
    private String gatewayTransactionId; // For idempotency

    @Column(name = "gateway_payment_method", length = 50)
    private String gatewayPaymentMethod;

    // Status
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    // Security & Audit
    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @Lob
    @Column(name = "webhook_signature", columnDefinition = "TEXT")
    private String webhookSignature;

    @Lob
    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Lob
    @Column(name = "dispute_reason", columnDefinition = "TEXT")
    private String disputeReason;

    @Lob
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Timestamps
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (expiresAt == null) {
            expiresAt = createdAt.plusHours(24); // 24 hours payment window
        }
        if (status == null) {
            status = PaymentStatus.PENDING;
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

    public Long getOfferId() {
        return offerId;
    }

    public void setOfferId(Long offerId) {
        this.offerId = offerId;
    }

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

    public BigDecimal getLawyerFee() {
        return lawyerFee;
    }

    public void setLawyerFee(BigDecimal lawyerFee) {
        this.lawyerFee = lawyerFee;
    }

    public BigDecimal getPlatformFee() {
        return platformFee;
    }

    public void setPlatformFee(BigDecimal platformFee) {
        this.platformFee = platformFee;
    }

    public BigDecimal getGatewayFee() {
        return gatewayFee;
    }

    public void setGatewayFee(BigDecimal gatewayFee) {
        this.gatewayFee = gatewayFee;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getLawyerPayout() {
        return lawyerPayout;
    }

    public void setLawyerPayout(BigDecimal lawyerPayout) {
        this.lawyerPayout = lawyerPayout;
    }

    public String getGateway() {
        return gateway;
    }

    public void setGateway(String gateway) {
        this.gateway = gateway;
    }

    public String getGatewayOrderId() {
        return gatewayOrderId;
    }

    public void setGatewayOrderId(String gatewayOrderId) {
        this.gatewayOrderId = gatewayOrderId;
    }

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    public void setGatewayTransactionId(String gatewayTransactionId) {
        this.gatewayTransactionId = gatewayTransactionId;
    }

    public String getGatewayPaymentMethod() {
        return gatewayPaymentMethod;
    }

    public void setGatewayPaymentMethod(String gatewayPaymentMethod) {
        this.gatewayPaymentMethod = gatewayPaymentMethod;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getWebhookSignature() {
        return webhookSignature;
    }

    public void setWebhookSignature(String webhookSignature) {
        this.webhookSignature = webhookSignature;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getDisputeReason() {
        return disputeReason;
    }

    public void setDisputeReason(String disputeReason) {
        this.disputeReason = disputeReason;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public LocalDateTime getSettledAt() {
        return settledAt;
    }

    public void setSettledAt(LocalDateTime settledAt) {
        this.settledAt = settledAt;
    }

    public LocalDateTime getRefundedAt() {
        return refundedAt;
    }

    public void setRefundedAt(LocalDateTime refundedAt) {
        this.refundedAt = refundedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
