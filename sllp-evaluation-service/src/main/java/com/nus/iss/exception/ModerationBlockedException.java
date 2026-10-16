package com.nus.iss.exception;

import lombok.Getter;

/** Raised when provider safety signals prohibit processing or releasing learner content. */
@Getter
public class ModerationBlockedException extends RuntimeException {
    private final String requestId;

    public ModerationBlockedException(String requestId) {
        super("content was blocked by safety moderation");
        this.requestId = requestId;
    }
}
