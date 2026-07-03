package com.jake.studysync.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
public class ExternalService {

    @Retryable(
            value = {RuntimeException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public String callExternalApi(String userId) {
        log.info("Calling external API for user: {}", userId);

        // Simulate random failures
        if (ThreadLocalRandom.current().nextInt(1, 5) == 3) {
            log.warn("External API call failed for user: {}", userId);
            throw new RuntimeException("External API temporarily unavailable");
        }

        // Simulate processing time
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return "Successfully processed for user: " + userId;
    }

    @Recover
    public String recoverExternalApi(RuntimeException e, String userId) {
        log.error("Recovery for user {} after all retries failed", userId, e);
        return "Fallback response for user: " + userId;
    }
}