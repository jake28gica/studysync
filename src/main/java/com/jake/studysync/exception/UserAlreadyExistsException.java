package com.jake.studysync.exception;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException( String message ) {
        super( message );
    }
}
