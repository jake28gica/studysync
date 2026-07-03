package com.jake.studysync.exception;

public class EmailServiceException extends RuntimeException {
  public EmailServiceException(String message) {
    super(message);
  }
}
