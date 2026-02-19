package com.legalconnect.lawyerbooking.controller;

import com.legalconnect.lawyerbooking.dto.CreatePaymentRequest;
import com.legalconnect.lawyerbooking.dto.PaymentResponseDTO;
import com.legalconnect.lawyerbooking.dto.VerifyPaymentRequest;
import com.legalconnect.lawyerbooking.security.UserPrincipal;
import com.legalconnect.lawyerbooking.service.PaymentService;
import com.legalconnect.lawyerbooking.service.RazorpayService;
import com.razorpay.RazorpayException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final RazorpayService razorpayService;

    public PaymentController(PaymentService paymentService, RazorpayService razorpayService) {
        this.paymentService = paymentService;
        this.razorpayService = razorpayService;
    }

    @PostMapping("/verify")
    public ResponseEntity<PaymentResponseDTO> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest request,
            Authentication authentication) {
        
        PaymentResponseDTO response = paymentService.verifyPayment(
                request.getRazorpayOrderId(), 
                request.getRazorpayPaymentId(), 
                request.getRazorpaySignature());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/create")
    public ResponseEntity<PaymentResponseDTO> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication) throws RazorpayException {
        
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Long userId = userPrincipal.getUserId();
        
        // Generate idempotency key if not provided
        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            idempotencyKey = UUID.randomUUID().toString();
        }
        
        PaymentResponseDTO payment = paymentService.createPayment(request, userId, idempotencyKey);
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponseDTO> getPaymentStatus(
            @PathVariable("paymentId") Long paymentId,
            Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Long userId = userPrincipal.getUserId();
        PaymentResponseDTO payment = paymentService.getPaymentStatus(paymentId, userId);
        return ResponseEntity.ok(payment);
    }
}
