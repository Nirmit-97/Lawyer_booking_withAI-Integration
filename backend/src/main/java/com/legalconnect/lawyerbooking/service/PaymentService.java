package com.legalconnect.lawyerbooking.service;

import com.legalconnect.lawyerbooking.entity.Case;
import com.legalconnect.lawyerbooking.entity.Payment;
import com.legalconnect.lawyerbooking.entity.Lawyer;
import com.legalconnect.lawyerbooking.enums.CaseStatus;
import com.legalconnect.lawyerbooking.exception.BadRequestException;
import com.legalconnect.lawyerbooking.exception.ResourceNotFoundException;
import com.legalconnect.lawyerbooking.repository.CaseRepository;
import com.legalconnect.lawyerbooking.repository.PaymentRepository;
import com.legalconnect.lawyerbooking.repository.LawyerRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class PaymentService {

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private LawyerRepository lawyerRepository;

    @Autowired
    private CaseService caseService; // For DTO conversion if needed, or updates

    private static final double ADMIN_COMMISSION_PERCENTAGE = 0.10; // 10%
    private static final double DEFAULT_CASE_FEE = 1000.00; // Example fixed fee, should be dynamic based on lawyer/case type

    @Transactional
    public Payment createOrder(Long caseId) throws RazorpayException {
        Case caseEntity = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found"));

        if (caseEntity.getLawyerId() == null) {
            throw new BadRequestException("No lawyer assigned to this case yet.");
        }
        
        // Check if payment already exists
        Optional<Payment> existingPayment = paymentRepository.findByCaseId(caseId);
        if (existingPayment.isPresent() && "PAID".equals(existingPayment.get().getStatus())) {
            throw new BadRequestException("Payment already made for this case.");
        }

        // Calculate amount
        double lawyerFee = caseEntity.getLawyerFee() != null ? caseEntity.getLawyerFee() : DEFAULT_CASE_FEE;
        double adminCommission = lawyerFee * ADMIN_COMMISSION_PERCENTAGE;
        double totalAmount = lawyerFee + adminCommission;

        RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", (int)(totalAmount * 100)); // Amount in paise
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "txn_" + caseId + "_" + System.currentTimeMillis());

        Order order = razorpay.orders.create(orderRequest);
        String orderId = order.get("id");

        Payment payment = existingPayment.orElse(new Payment());
        payment.setCaseId(caseId);
        payment.setUserId(caseEntity.getUserId());
        payment.setLawyerId(caseEntity.getLawyerId());
        payment.setAmount(totalAmount); // Total amount paid by user
        payment.setAdminCommission(adminCommission);
        payment.setLawyerFee(lawyerFee);
        payment.setRazorpayOrderId(orderId);
        payment.setStatus("CREATED");

        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment verifyPayment(String orderId, String paymentId, String signature) throws RazorpayException {
        Payment payment = paymentRepository.findByRazorpayOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment order not found"));

        JSONObject options = new JSONObject();
        options.put("razorpay_order_id", orderId);
        options.put("razorpay_payment_id", paymentId);
        options.put("razorpay_signature", signature);

        boolean isValid = Utils.verifyPaymentSignature(options, razorpayKeySecret);

        if (isValid) {
            payment.setRazorpayPaymentId(paymentId);
            payment.setRazorpaySignature(signature);
            payment.setStatus("PAID");
            Payment savedPayment = paymentRepository.save(payment);

            // Update Case status through CaseService to handle notifications etc.
            // But CaseService.updateCaseStatus takes String status.
            // We can also update repository directly here or use CaseService method.
            // CaseService method is better for consistency.
            // However, doing it inside this transaction is safer.
            
            Case caseEntity = caseRepository.findById(payment.getCaseId())
                     .orElseThrow(() -> new ResourceNotFoundException("Case not found"));
            
            caseEntity.setCaseStatus(CaseStatus.PAYMENT_RECEIVED);
            // After payment received, consultation starts, so maybe move to IN_PROGRESS?
            // The prompt says: Case status -> Payment Received -> Consultation starts
            // So we set it to PAYMENT_RECEIVED.
            
            caseRepository.save(caseEntity);
            
            // Should probably notify lawyer/user via websocket here too, similar to CaseService
            
            return savedPayment;
        } else {
            payment.setStatus("FAILED");
            paymentRepository.save(payment);
            throw new BadRequestException("Payment signature verification failed");
        }
    }
}
