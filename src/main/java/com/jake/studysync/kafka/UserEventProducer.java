package com.jake.studysync.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jake.studysync.event.UserEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
@RequiredArgsConstructor
public class UserEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOPIC = "user-registration-events";

    public void sendUserEvent( UserEvent event) {
        try {
            String eventJson = objectMapper.writeValueAsString(event);

            CompletableFuture<SendResult<String, String>> future =
                    kafkaTemplate.send(TOPIC, event.getUserId(), eventJson);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("User event sent successfully: {}", event);
                } else {
                    log.error("Failed to send user event: {}", event, ex);
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize user event", e);
        }
    }
}