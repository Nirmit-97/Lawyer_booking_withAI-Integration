package com.legalconnect.lawyerbooking.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "client_audio")
public class ClientAudio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "lawyer_id")
    private Long lawyerId;

    @Column(name = "appointment_id")
    private Long appointmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", insertable = false, updatable = false)
    private Case caseEntity;

    @Column(name = "case_id")
    private Long caseId;

    private String language;

    @Lob
    @Column(name = "original_english_text", columnDefinition = "LONGTEXT")
    private String originalEnglishText;

    @Lob
    @Column(name = "masked_english_text", columnDefinition = "LONGTEXT")
    private String maskedEnglishText;

    @Lob
    @Column(name = "masked_text_audio", columnDefinition = "LONGBLOB")
    private byte[] maskedTextAudio;

    @Lob
    @Column(name = "masked_gujarati_text", columnDefinition = "LONGTEXT")
    private String maskedGujaratiText;

    @Lob
    @Column(name = "masked_gujarati_audio", columnDefinition = "LONGBLOB")
    private byte[] maskedGujaratiAudio;

    // getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getOriginalEnglishText() { return originalEnglishText; }
    public void setOriginalEnglishText(String originalEnglishText) {
        this.originalEnglishText = originalEnglishText;
    }

    public String getMaskedEnglishText() { return maskedEnglishText; }
    public void setMaskedEnglishText(String maskedEnglishText) {
        this.maskedEnglishText = maskedEnglishText;
    }

    public byte[] getMaskedTextAudio() { return maskedTextAudio; }
    public void setMaskedTextAudio(byte[] maskedTextAudio) {
        this.maskedTextAudio = maskedTextAudio;
    }

    public String getMaskedGujaratiText() { return maskedGujaratiText; }
    public void setMaskedGujaratiText(String maskedGujaratiText) {
        this.maskedGujaratiText = maskedGujaratiText;
    }

    public byte[] getMaskedGujaratiAudio() { return maskedGujaratiAudio; }
    public void setMaskedGujaratiAudio(byte[] maskedGujaratiAudio) {
        this.maskedGujaratiAudio = maskedGujaratiAudio;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getLawyerId() {
        return lawyerId;
    }

    public void setLawyerId(Long lawyerId) {
        this.lawyerId = lawyerId;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(Long appointmentId) {
        this.appointmentId = appointmentId;
    }

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }

    public Case getCaseEntity() {
        return caseEntity;
    }

    public void setCaseEntity(Case caseEntity) {
        this.caseEntity = caseEntity;
        if (caseEntity != null) {
            this.caseId = caseEntity.getId();
        }
    }
}
