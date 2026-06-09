package com.nus.iss.user.exception;

/**
 * Exception thrown when credentials are invalid.
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
