package com.jake.studysync.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
public class EmailService {

    @Retryable(
            value = {Exception.class},
            maxAttempts = 4,
            backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 8000)
    )
    public void sendEmail(String to, String subject, String body) {
        log.info("Sending email to: {}", to);

        // Simulate API call with potential failure
        if (ThreadLocalRandom.current().nextInt(1, 6) == 3) {
            throw new RuntimeException("Email service temporarily unavailable");
        }

        // Simulate network delay
        try {
            Thread.sleep(500 + ThreadLocalRandom.current().nextInt(500));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("Email sent successfully to: {}", to);
    }

    @Recover
    public void recoverSendEmail(Exception e, String to, String subject, String body) {
        log.error("Failed to send email to {} after all retries. Error: {}", to, e.getMessage());

        // Store in a queue for later processing
        // Or send to a dead letter queue
        // Or log for manual processing
        log.warn("Email queued for manual processing: to={}, subject={}", to, subject);
    }
}