package com.legalconnect.lawyerbooking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

public class VerifyPaymentRequest {
    @NotBlank
    private String razorpayPaymentId;
    
    @NotBlank
    private String razorpayOrderId;
    
    @NotBlank
    private String razorpaySignature;

    public String getRazorpayPaymentId() {
        return razorpayPaymentId;
    }

    public void setRazorpayPaymentId(String razorpayPaymentId) {
        this.razorpayPaymentId = razorpayPaymentId;
    }

    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public void setRazorpayOrderId(String razorpayOrderId) {
        this.razorpayOrderId = razorpayOrderId;
    }

    public String getRazorpaySignature() {
        return razorpaySignature;
    }

    public void setRazorpaySignature(String razorpaySignature) {
        this.razorpaySignature = razorpaySignature;
    }
}
