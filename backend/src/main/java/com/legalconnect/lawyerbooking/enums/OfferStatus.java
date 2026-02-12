package com.legalconnect.lawyerbooking.enums;

public enum OfferStatus {
    SUBMITTED,      // Lawyer submitted offer, pending user review
    ACCEPTED,       // User accepted this offer (case moves to PAYMENT_PENDING)
    REJECTED,       // User selected another offer, this one was rejected
    EXPIRED,        // Offer expired after 48 hours without acceptance
    WITHDRAWN,      // Lawyer withdrew offer before acceptance
    FUNDED          // Payment completed for this offer, case in progress
}
