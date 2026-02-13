package com.legalconnect.lawyerbooking.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.apache.commons.codec.digest.HmacUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class RazorpayService {

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.webhook.secret:}")
    private String webhookSecret;

    public String createOrder(BigDecimal totalAmount, Long caseId, Long offerId) throws RazorpayException {
        RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

        // Convert to paise (Razorpay uses smallest currency unit)
        int amountInPaise = totalAmount.multiply(new BigDecimal(100)).intValue();

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "case_" + caseId + "_offer_" + offerId);

        JSONObject notes = new JSONObject();
        notes.put("caseId", caseId.toString());
        notes.put("offerId", offerId.toString());
        orderRequest.put("notes", notes);

        Order order = razorpayClient.orders.create(orderRequest);
        return order.get("id");
    }

    public boolean verifyWebhookSignature(String webhookBody, String signature) {
        if (webhookSecret == null || webhookSecret.isEmpty()) {
            throw new RuntimeException("Webhook secret not configured");
        }

        String generatedSignature = new HmacUtils("HmacSHA256", webhookSecret).hmacHex(webhookBody);
        return generatedSignature.equals(signature);
    }

    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        String payload = orderId + "|" + paymentId;
        String generatedSignature = new HmacUtils("HmacSHA256", razorpayKeySecret).hmacHex(payload);
        return generatedSignature.equals(signature);
    }

    public String getRazorpayKeyId() {
        return razorpayKeyId;
    }
}
