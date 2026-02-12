package com.legalconnect.lawyerbooking.enums;

public enum PaymentStatus {
    PENDING,        // Payment order created, awaiting gateway confirmation
    SUCCESS,        // Webhook confirmed payment success
    FAILED,         // Payment failed at gateway
    EXPIRED,        // Payment link expired (24 hours timeout)
    REFUNDED,       // Payment refunded to user
    DISPUTED,       // Payment disputed by user or lawyer
    SETTLED         // Payout completed to lawyer
}
