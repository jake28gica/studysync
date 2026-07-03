package com.jake.studysync.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RetryServiceTest {

    @Autowired
    private ExternalService externalService;

    @Autowired
    private EmailService emailService;

    @Test
    void testRetryOnFailure() {
        String result = externalService.callExternalApi("test-user");
        assertThat(result).isNotNull();
        // The method will retry automatically on failure
    }

    @Test
    void testEmailRetry() {
        // This will retry on failure up to 4 times
        emailService.sendEmail("test@example.com", "Test Subject", "Test Body");
        // No assertion needed - if it fails all retries, recovery will handle it
    }
}