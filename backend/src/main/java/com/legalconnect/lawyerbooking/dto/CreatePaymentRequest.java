package com.legalconnect.lawyerbooking.dto;

import jakarta.validation.constraints.NotNull;

public class CreatePaymentRequest {
    
    @NotNull(message = "Offer ID is required")
    private Long offerId;

    // Constructors
    public CreatePaymentRequest() {}

    public CreatePaymentRequest(Long offerId) {
        this.offerId = offerId;
    }

    // Getters and Setters
    public Long getOfferId() {
        return offerId;
    }

    public void setOfferId(Long offerId) {
        this.offerId = offerId;
    }
}
