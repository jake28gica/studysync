package com.jake.studysync.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jake.studysync.event.UserEvent;
import com.jake.studysync.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserEventConsumer {

    private final ObjectMapper        objectMapper;
    private final NotificationService notificationService;
    private final Random              random = new Random();

    @KafkaListener(topics = "user-registration-events", groupId = "user-event-processor")
    public void processUserEvent(String message) {
        try {
            // Simulate long processing (3-5 seconds)
            int processingTime = 3000 + random.nextInt(2000);
            Thread.sleep(processingTime);

            UserEvent event = objectMapper.readValue( message, UserEvent.class);
            log.info("Processing user event: {} (took {} ms)", event, processingTime);

            // Process different event types
            switch (event.getEventType()) {
                case REGISTERED:
                    notificationService.sendWelcomeEmail(event.getEmail());
                    break;
                case LOGIN:
                    notificationService.sendLoginNotification(event.getEmail());
                    break;
                case LOGOUT:
                    notificationService.sendLogoutNotification(event.getEmail());
                    break;
                default:
                    log.debug("Received unhandled event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("Error processing user event: {}", message, e);
        }
    }
}