package com.legalconnect.lawyerbooking.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "otp_tokens")
public class OtpToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 10)
    private String otp;

    @Column(nullable = false)
    private String userType; // "USER" or "LAWYER"

    @Column(nullable = false)
    private LocalDateTime expiryTime;

    @Column(nullable = false)
    private boolean used = false;

    // Constructors
    public OtpToken() {}

    public OtpToken(String email, String otp, String userType, LocalDateTime expiryTime) {
        this.email = email;
        this.otp = otp;
        this.userType = userType;
        this.expiryTime = expiryTime;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }

    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }

    public LocalDateTime getExpiryTime() { return expiryTime; }
    public void setExpiryTime(LocalDateTime expiryTime) { this.expiryTime = expiryTime; }

    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }
}
