package com.legalconnect.lawyerbooking.controller;

import com.legalconnect.lawyerbooking.service.PaymentService;
import com.legalconnect.lawyerbooking.service.RazorpayService;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentWebhookController {

    private final PaymentService paymentService;
    private final RazorpayService razorpayService;

    public PaymentWebhookController(PaymentService paymentService, RazorpayService razorpayService) {
        this.paymentService = paymentService;
        this.razorpayService = razorpayService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {
        
        try {
            // Verify signature
            if (!razorpayService.verifyWebhookSignature(payload, signature)) {
                return ResponseEntity.status(401).body("Invalid signature");
            }

            // Parse webhook payload
            JSONObject webhookData = new JSONObject(payload);
            String event = webhookData.getString("event");
            JSONObject paymentEntity = webhookData.getJSONObject("payload")
                    .getJSONObject("payment").getJSONObject("entity");

            String razorpayPaymentId = paymentEntity.getString("id");
            String razorpayOrderId = paymentEntity.getString("order_id");

            // Handle different events
            switch (event) {
                case "payment.captured":
                    paymentService.handlePaymentSuccess(razorpayPaymentId, razorpayOrderId, signature);
                    break;
                case "payment.failed":
                    String errorReason = paymentEntity.optString("error_description", "Payment failed");
                    paymentService.handlePaymentFailure(razorpayOrderId, errorReason);
                    break;
                default:
                    // Ignore other events
                    break;
            }

            return ResponseEntity.ok("Webhook processed");
        } catch (Exception e) {
            // Log error but return 200 to prevent Razorpay retries
            e.printStackTrace();
            return ResponseEntity.ok("Webhook received");
        }
    }
}
