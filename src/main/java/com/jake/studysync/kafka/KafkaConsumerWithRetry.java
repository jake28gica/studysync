package com.jake.studysync.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jake.studysync.event.UserEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaConsumerWithRetry {

    private final ObjectMapper objectMapper;
    private final Random random = new Random();

    @KafkaListener(topics = "user-registration-events", groupId = "user-event-processor")
    @Retryable(
            value = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2, maxDelay = 10000)
    )
    public void processUserEventWithRetry(String message) throws Exception {
        log.info("Processing Kafka message with retry: {}", message);

        // Simulate processing with occasional failures
        int randomValue = random.nextInt(10);
        if (randomValue < 3) { // 30% chance of failure
            log.warn("Simulated failure for message: {}", message);
            throw new Exception("Processing failed - will retry");
        }

        // Simulate long processing (3-5 seconds)
        int processingTime = 3000 + random.nextInt(2000);
        Thread.sleep(processingTime);

        UserEvent event = objectMapper.readValue( message, UserEvent.class);
        log.info("Successfully processed user event: {} (took {} ms)", event, processingTime);

        // Process different event types
        processEvent(event);
    }

    private void processEvent(UserEvent event) {
        switch (event.getEventType()) {
            case REGISTERED:
                log.info("Processing registration for user: {}", event.getEmail());
                // Simulate business logic
                break;
            case LOGIN:
                log.info("Processing login for user: {}", event.getEmail());
                break;
            case LOGOUT:
                log.info("Processing logout for user: {}", event.getEmail());
                break;
            case UPDATED:
                log.info("Processing update for user: {}", event.getEmail());
                break;
            default:
                log.debug("Received unhandled event type: {}", event.getEventType());
        }
    }

    @Recover
    public void recoverUserEvent(Exception e, String message) {
        log.error("All retry attempts failed for message: {}. Error: {}", message, e.getMessage());

        // Send to dead letter queue or log for manual intervention
        try {
            UserEvent event = objectMapper.readValue(message, UserEvent.class);
            log.error("Failed to process user event after all retries: {}", event);

            // Here you could:
            // 1. Send to a dead letter topic
            // 2. Store in database for manual processing
            // 3. Send alert/notification
        } catch (Exception ex) {
            log.error("Failed to parse failed message: {}", message, ex);
        }
    }
}