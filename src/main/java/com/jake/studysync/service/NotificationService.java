package com.jake.studysync.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final EmailService emailService;

    @Retryable(
            value = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1500, multiplier = 1.5)
    )
    public void sendWelcomeEmail(String email) {
        log.info("Sending welcome email to: {}", email);
        String subject = "Welcome to Social Auth Service";
        String body = "Thank you for registering with us!";
        emailService.sendEmail(email, subject, body);
    }

    @Retryable(
            value = {Exception.class},
            maxAttempts = 2,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void sendLoginNotification(String email) {
        log.info("Sending login notification to: {}", email);
        String subject = "New Login Detected";
        String body = "A new login was detected on your account.";
        emailService.sendEmail(email, subject, body);
    }

    public void sendLogoutNotification(String email) {
        log.info("Sending logout notification to: {}", email);
        // Simple notification without retry needed
        String subject = "Logout Detected";
        String body = "You have been logged out successfully.";
        try {
            emailService.sendEmail(email, subject, body);
        } catch (Exception e) {
            log.warn("Failed to send logout notification to: {}", email, e);
        }
    }

    @Recover
    public void recoverSendWelcomeEmail(Exception e, String email) {
        log.error("Failed to send welcome email to {} after retries", email, e);
        // Store in database for retry later
        // Or send to a queue for async processing
    }
}