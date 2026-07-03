package com.jake.studysync.event;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserEvent {
    String userId;
    String email;
    EventType eventType;
    LocalDateTime timestamp;
    String source = "social-auth-service";

    public enum EventType {
        REGISTERED,
        LOGIN,
        LOGOUT,
        UPDATED,
        PASSWORD_RESET
    }
}