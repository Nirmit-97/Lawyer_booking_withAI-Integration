package com.legalconnect.lawyerbooking.service;

import com.legalconnect.lawyerbooking.dto.CreatePaymentRequest;
import com.legalconnect.lawyerbooking.dto.PaymentResponseDTO;
import com.legalconnect.lawyerbooking.entity.Case;
import com.legalconnect.lawyerbooking.entity.Offer;
import com.legalconnect.lawyerbooking.entity.Payment;
import com.legalconnect.lawyerbooking.enums.CaseStatus;
import com.legalconnect.lawyerbooking.enums.OfferStatus;
import com.legalconnect.lawyerbooking.enums.PaymentStatus;
import com.legalconnect.lawyerbooking.repository.CaseRepository;
import com.legalconnect.lawyerbooking.repository.OfferRepository;
import com.legalconnect.lawyerbooking.repository.PaymentRepository;
import com.razorpay.RazorpayException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OfferRepository offerRepository;
    private final CaseRepository caseRepository;
    private final RazorpayService razorpayService;

    @Value("${payment.platform.fee.percentage:10}")
    private int platformFeePercentage;

    @Value("${payment.gateway.fee.percentage:2}")
    private int gatewayFeePercentage;

    public PaymentService(PaymentRepository paymentRepository, OfferRepository offerRepository,
                          CaseRepository caseRepository, RazorpayService razorpayService) {
        this.paymentRepository = paymentRepository;
        this.offerRepository = offerRepository;
        this.caseRepository = caseRepository;
        this.razorpayService = razorpayService;
    }

    @Transactional
    public PaymentResponseDTO createPayment(CreatePaymentRequest request, Long userId, String idempotencyKey) throws RazorpayException {
        // Check idempotency
        if (paymentRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            Payment existing = paymentRepository.findByIdempotencyKey(idempotencyKey).get();
            return convertToDTO(existing);
        }

        Offer offer = offerRepository.findById(request.getOfferId())
                .orElseThrow(() -> new RuntimeException("Offer not found"));

        Case caseEntity = caseRepository.findById(offer.getCaseId())
                .orElseThrow(() -> new RuntimeException("Case not found"));

        // Validate user owns the case
        if (!caseEntity.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        // Validate case status
        if (caseEntity.getCaseStatus() != CaseStatus.PAYMENT_PENDING) {
            throw new RuntimeException("Case is not in PAYMENT_PENDING status");
        }

        // Validate offer is accepted
        if (offer.getStatus() != OfferStatus.ACCEPTED) {
            throw new RuntimeException("Offer is not accepted");
        }

        // Check for existing pending payment
        java.util.Optional<Payment> existingPending = paymentRepository.findByCaseIdAndStatus(caseEntity.getId(), PaymentStatus.PENDING);
        if (existingPending.isPresent()) {
            return convertToDTO(existingPending.get());
        }

        // Calculate fees
        BigDecimal lawyerFee = offer.getProposedFee();
        BigDecimal gatewayFee = lawyerFee.multiply(new BigDecimal(gatewayFeePercentage))
                .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = lawyerFee.add(gatewayFee);
        BigDecimal platformFee = lawyerFee.multiply(new BigDecimal(platformFeePercentage))
                .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
        BigDecimal lawyerPayout = lawyerFee.subtract(platformFee);

        // Create Razorpay order
        String razorpayOrderId = razorpayService.createOrder(totalAmount, caseEntity.getId(), offer.getId());

        // Create payment record
        Payment payment = new Payment();
        payment.setCaseId(caseEntity.getId());
        payment.setOfferId(offer.getId());
        payment.setLawyerId(offer.getLawyerId());
        payment.setUserId(userId);
        payment.setLawyerFee(lawyerFee);
        payment.setPlatformFee(platformFee);
        payment.setGatewayFee(gatewayFee);
        payment.setTotalAmount(totalAmount);
        payment.setLawyerPayout(lawyerPayout);
        payment.setGatewayOrderId(razorpayOrderId);
        payment.setIdempotencyKey(idempotencyKey);
        payment.setStatus(PaymentStatus.PENDING);

        Payment savedPayment = paymentRepository.save(payment);

        return convertToDTO(savedPayment);
    }

    @Transactional
    public void handlePaymentSuccess(String razorpayPaymentId, String razorpayOrderId, String razorpaySignature) {
        // Check idempotency
        if (paymentRepository.existsByGatewayTransactionId(razorpayPaymentId)) {
            return; // Already processed
        }

        Payment payment = paymentRepository.findByGatewayOrderId(razorpayOrderId)
                .orElseThrow(() -> new RuntimeException("Payment not found for order: " + razorpayOrderId));

        // Update payment
        payment.setGatewayTransactionId(razorpayPaymentId);
        payment.setWebhookSignature(razorpaySignature);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // Update offer
        Offer offer = offerRepository.findById(payment.getOfferId())
                .orElseThrow(() -> new RuntimeException("Offer not found"));
        offer.setStatus(OfferStatus.FUNDED);
        offerRepository.save(offer);

        // Update case
        Case caseEntity = caseRepository.findById(payment.getCaseId())
                .orElseThrow(() -> new RuntimeException("Case not found"));
        caseEntity.setCaseStatus(CaseStatus.IN_PROGRESS);
        caseRepository.save(caseEntity);
    }

    @Transactional
    public void handlePaymentFailure(String razorpayOrderId, String reason) {
        Payment payment = paymentRepository.findByGatewayOrderId(razorpayOrderId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(reason);
        paymentRepository.save(payment);

        // Update case status
        Case caseEntity = caseRepository.findById(payment.getCaseId())
                .orElseThrow(() -> new RuntimeException("Case not found"));
        caseEntity.setCaseStatus(CaseStatus.PAYMENT_FAILED);
        caseRepository.save(caseEntity);
    }

    public PaymentResponseDTO getPaymentStatus(Long paymentId, Long userId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (!payment.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        return convertToDTO(payment);
    }

    private PaymentResponseDTO convertToDTO(Payment payment) {
        PaymentResponseDTO dto = new PaymentResponseDTO();
        dto.setId(payment.getId());
        dto.setCaseId(payment.getCaseId());
        dto.setOfferId(payment.getOfferId());
        dto.setLawyerFee(payment.getLawyerFee());
        dto.setPlatformFee(payment.getPlatformFee());
        dto.setGatewayFee(payment.getGatewayFee());
        dto.setTotalAmount(payment.getTotalAmount());
        dto.setGateway(payment.getGateway());
        dto.setGatewayOrderId(payment.getGatewayOrderId());
        dto.setRazorpayKeyId(razorpayService.getRazorpayKeyId());
        dto.setStatus(payment.getStatus());
        dto.setCreatedAt(payment.getCreatedAt());
        dto.setExpiresAt(payment.getExpiresAt());
        dto.setFailureReason(payment.getFailureReason());
        return dto;
    }
}
