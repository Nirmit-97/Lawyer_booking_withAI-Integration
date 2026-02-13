package com.legalconnect.lawyerbooking.repository;

import com.legalconnect.lawyerbooking.entity.Payment;
import com.legalconnect.lawyerbooking.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    // Find payment by Razorpay order ID
    Optional<Payment> findByGatewayOrderId(String gatewayOrderId);
    
    // Find payment by Razorpay transaction ID (for idempotency)
    Optional<Payment> findByGatewayTransactionId(String transactionId);
    
    // Find payment by idempotency key
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    
    // Find payment for a case with specific status
    Optional<Payment> findByCaseIdAndStatus(Long caseId, PaymentStatus status);
    
    // Find expired payments for scheduled cleanup
    List<Payment> findByStatusAndExpiresAtBefore(PaymentStatus status, LocalDateTime now);
    
    // Check if transaction already processed (idempotency check)
    boolean existsByGatewayTransactionId(String transactionId);
    
    // Find all payments for a case
    List<Payment> findByCaseIdOrderByCreatedAtDesc(Long caseId);
    
    // Find all payments for a user
    List<Payment> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    // Find all payments for a lawyer
    List<Payment> findByLawyerIdOrderByCreatedAtDesc(Long lawyerId);
    
    // Find payments by status
    List<Payment> findByStatus(PaymentStatus status);
}
