package com.legalconnect.lawyerbooking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired(required = false)
    private JavaMailSender emailSender;

    @Value("${spring.mail.username:noreply@legalconnect.com}")
    private String fromEmail;

    /**
     * Send a simple email notification asynchronously.
     */
    @Async
    public void sendEmail(String to, String subject, String text) {
        if (emailSender == null) {
            logger.warn("JavaMailSender is not configured. Skipping email to: {}", to);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            emailSender.send(message);
            logger.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            logger.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    /**
     * Send notification for new case assignment
     */
    public void notifyLawyerOfNewCase(String lawyerEmail, Long caseId, String caseTitle) {
        String subject = "New Case Assigned: " + caseTitle;
        String text = "You have been assigned a new case (ID: " + caseId + ").\n\nTitle: " + caseTitle + "\n\nPlease log in to the portal to view details.";
        sendEmail(lawyerEmail, subject, text);
    }

    /**
     * Send notification for lawyer's solution
     */
    public void notifyUserOfSolution(String userEmail, Long caseId, String caseTitle) {
        String subject = "Update on Case: " + caseTitle;
        String text = "A lawyer has provided a solution/update for your case (ID: " + caseId + ").\n\nPlease log in to view the details.";
        sendEmail(userEmail, subject, text);
    }
}
