package com.legalconnect.lawyerbooking.enums;

public enum CaseStatus {
    DRAFT,              // Case created from audio, awaiting user review/publish
    PUBLISHED,          // Case published by user, visible to lawyers
    OPEN,               // Legacy status for backward compatibility
    UNDER_REVIEW,       // Case has offers pending user review
    PAYMENT_PENDING,    // User accepted an offer, payment initiated but not confirmed
    PAYMENT_FAILED,     // Payment expired or failed, needs retry
    IN_PROGRESS,        // Case actively being worked on by lawyer (payment confirmed)
    CLOSED,             // Case resolved
    ON_HOLD,            // Case temporarily paused
    VERIFIED,           // Case verified by admin
    PENDING_APPROVAL    // Legacy: Lawyer accepted, awaiting user approval (deprecated)
}
