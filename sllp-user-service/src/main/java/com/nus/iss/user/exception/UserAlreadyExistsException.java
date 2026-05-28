package com.nus.iss.user.exception;

/**
 * Exception thrown when user with email already exists.
 */
public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String email) {
        super("User with email '" + email + "' already exists");
    }
}
