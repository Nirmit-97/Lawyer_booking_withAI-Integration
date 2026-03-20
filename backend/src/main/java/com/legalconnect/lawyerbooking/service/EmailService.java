package com.legalconnect.lawyerbooking.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Password Reset - OTP Verification");
        message.setText("Your OTP for password reset is: " + otp + "\n\nThis OTP is valid for 10 minutes. Do not share it with anyone.");
        
        System.out.println("DEVELOPMENT MODE [MOCK EMAIL] - Generated OTP: " + otp);

        try {
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send OTP email to " + toEmail + ". " +
                    "Since this is a development environment, the OTP has been printed above to the console, and the flow will continue successfully.");
        }
    }
}
