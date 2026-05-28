package com.nus.iss.user.exception;

/**
 * Exception thrown when user is not found.
 */
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String email) {
        super("User with email '" + email + "' not found");
    }

    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
