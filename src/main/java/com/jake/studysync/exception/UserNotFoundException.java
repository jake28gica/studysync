package com.jake.studysync.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException( String message ) {
        super( message );
    }
}
