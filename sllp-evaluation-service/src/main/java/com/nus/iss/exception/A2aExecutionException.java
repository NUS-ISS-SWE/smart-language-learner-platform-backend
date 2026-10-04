package com.nus.iss.exception;

import lombok.Getter;

@Getter
public class A2aExecutionException extends RuntimeException {
    private final String requestId;

    public A2aExecutionException(String requestId, String message) {
        super(message);
        this.requestId = requestId;
    }

    public A2aExecutionException(String requestId, String message, Throwable cause) {
        super(message, cause);
        this.requestId = requestId;
    }
}
