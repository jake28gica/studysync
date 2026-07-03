package com.jake.studysync.exception;

public class RateLimitExceededException extends RuntimeException {
  public RateLimitExceededException(String message) {
    super(message);
  }
}
