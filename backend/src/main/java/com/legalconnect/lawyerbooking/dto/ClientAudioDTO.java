package com.legalconnect.lawyerbooking.dto;

import java.util.Base64;

public class ClientAudioDTO {
    private Long id;
    private String language;
    private String originalEnglishText;
    private String maskedEnglishText;
    private String maskedTextAudioBase64; // Base64 encoded audio
    private String maskedGujaratiText;
    private String maskedGujaratiAudioBase64; // Base64 encoded Gujarati audio
    private Long userId;
    private Long caseId;
    private String caseTitle;
    private Long lawyerId;

    // Constructors
    public ClientAudioDTO() {}

    public ClientAudioDTO(Long id, String language, String originalEnglishText, 
                         String maskedEnglishText, byte[] maskedTextAudio,
                         String maskedGujaratiText, byte[] maskedGujaratiAudio,
                         Long userId, Long caseId, Long lawyerId) {
        this.id = id;
        this.language = language;
        this.originalEnglishText = originalEnglishText;
        this.maskedEnglishText = maskedEnglishText;
        if (maskedTextAudio != null && maskedTextAudio.length > 0) {
            this.maskedTextAudioBase64 = Base64.getEncoder().encodeToString(maskedTextAudio);
        } else {
            this.maskedTextAudioBase64 = null;
        }
        this.maskedGujaratiText = maskedGujaratiText;
        if (maskedGujaratiAudio != null && maskedGujaratiAudio.length > 0) {
            this.maskedGujaratiAudioBase64 = Base64.getEncoder().encodeToString(maskedGujaratiAudio);
        } else {
            this.maskedGujaratiAudioBase64 = null;
        }
        this.userId = userId;
        this.caseId = caseId;
        this.lawyerId = lawyerId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getOriginalEnglishText() {
        return originalEnglishText;
    }

    public void setOriginalEnglishText(String originalEnglishText) {
        this.originalEnglishText = originalEnglishText;
    }

    public String getMaskedEnglishText() {
        return maskedEnglishText;
    }

    public void setMaskedEnglishText(String maskedEnglishText) {
        this.maskedEnglishText = maskedEnglishText;
    }

    public String getMaskedTextAudioBase64() {
        return maskedTextAudioBase64;
    }

    public void setMaskedTextAudioBase64(String maskedTextAudioBase64) {
        this.maskedTextAudioBase64 = maskedTextAudioBase64;
    }

    public void setMaskedTextAudio(byte[] maskedTextAudio) {
        if (maskedTextAudio != null && maskedTextAudio.length > 0) {
            this.maskedTextAudioBase64 = Base64.getEncoder().encodeToString(maskedTextAudio);
        } else {
            this.maskedTextAudioBase64 = null;
        }
    }

    public String getMaskedGujaratiText() {
        return maskedGujaratiText;
    }

    public void setMaskedGujaratiText(String maskedGujaratiText) {
        this.maskedGujaratiText = maskedGujaratiText;
    }

    public String getMaskedGujaratiAudioBase64() {
        return maskedGujaratiAudioBase64;
    }

    public void setMaskedGujaratiAudioBase64(String maskedGujaratiAudioBase64) {
        this.maskedGujaratiAudioBase64 = maskedGujaratiAudioBase64;
    }

    public void setMaskedGujaratiAudio(byte[] maskedGujaratiAudio) {
        if (maskedGujaratiAudio != null && maskedGujaratiAudio.length > 0) {
            this.maskedGujaratiAudioBase64 = Base64.getEncoder().encodeToString(maskedGujaratiAudio);
        } else {
            this.maskedGujaratiAudioBase64 = null;
        }
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }

    public String getCaseTitle() {
        return caseTitle;
    }

    public void setCaseTitle(String caseTitle) {
        this.caseTitle = caseTitle;
    }

    public Long getLawyerId() {
        return lawyerId;
    }

    public void setLawyerId(Long lawyerId) {
        this.lawyerId = lawyerId;
    }
}

