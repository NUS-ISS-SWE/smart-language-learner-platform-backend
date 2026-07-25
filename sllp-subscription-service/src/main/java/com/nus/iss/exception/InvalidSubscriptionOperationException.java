package com.nus.iss.exception;

public class InvalidSubscriptionOperationException extends RuntimeException {
    public InvalidSubscriptionOperationException(String message) {
        super(message);
    }
}
