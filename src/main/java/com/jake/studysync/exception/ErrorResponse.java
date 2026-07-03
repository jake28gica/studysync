package com.jake.studysync.exception;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
public class ErrorResponse {
    // Getters
    private String              message;
    private int                 status;
    private LocalDateTime       timestamp;
    private Map<String, String> details;

    public ErrorResponse(String message, int status) {
        this.message = message;
        this.status = status;
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponse(String message, int status, Map<String, String> details) {
        this.message = message;
        this.status = status;
        this.timestamp = LocalDateTime.now();
        this.details = details;
    }

}