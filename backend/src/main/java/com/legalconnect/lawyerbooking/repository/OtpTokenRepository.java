package com.legalconnect.lawyerbooking.repository;

import com.legalconnect.lawyerbooking.entity.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {
    Optional<OtpToken> findByEmailAndOtpAndUserTypeAndUsedFalse(String email, String otp, String userType);
    Optional<OtpToken> findFirstByEmailAndUserTypeOrderByExpiryTimeDesc(String email, String userType);
}
