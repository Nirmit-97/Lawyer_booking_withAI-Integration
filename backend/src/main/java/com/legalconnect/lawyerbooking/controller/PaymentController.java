package com.legalconnect.lawyerbooking.controller;

import com.legalconnect.lawyerbooking.entity.Payment;
import com.legalconnect.lawyerbooking.service.PaymentService;
import com.razorpay.RazorpayException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*", allowedHeaders = "*") // Allow frontend access
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @org.springframework.beans.factory.annotation.Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @PostMapping("/create-order/{caseId}")
    public ResponseEntity<?> createOrder(@PathVariable Long caseId) {
        try {
            Payment payment = paymentService.createOrder(caseId);
            return ResponseEntity.ok(Map.of(
                "orderId", payment.getRazorpayOrderId(),
                "amount", payment.getAmount(),
                "currency", "INR",
                "keyId", razorpayKeyId
            ));
        } catch (RazorpayException e) {
            return ResponseEntity.status(500).body("Error creating Razorpay order: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> data) {
        try {
            String orderId = data.get("razorpay_order_id");
            String paymentId = data.get("razorpay_payment_id");
            String signature = data.get("razorpay_signature");

            Payment payment = paymentService.verifyPayment(orderId, paymentId, signature);
            return ResponseEntity.ok(Map.of("message", "Payment verified successfully", "status", payment.getStatus()));
        } catch (RazorpayException e) {
             return ResponseEntity.status(500).body("Error verifying Razorpay payment: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
