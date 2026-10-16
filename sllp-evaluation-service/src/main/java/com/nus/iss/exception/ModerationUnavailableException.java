package com.nus.iss.exception;

import lombok.Getter;

/** Raised when moderation cannot make a trustworthy decision; callers must fail closed. */
@Getter
public class ModerationUnavailableException extends RuntimeException {
    private final String requestId;

    public ModerationUnavailableException(String requestId, Throwable cause) {
        super("content safety verification is temporarily unavailable", cause);
        this.requestId = requestId;
    }
}
