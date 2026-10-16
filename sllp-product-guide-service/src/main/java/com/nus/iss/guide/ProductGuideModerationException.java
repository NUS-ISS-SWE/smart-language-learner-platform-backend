package com.nus.iss.guide;

public class ProductGuideModerationException extends RuntimeException {
    public enum Reason { BLOCKED, UNAVAILABLE }

    private final Reason reason;

    public ProductGuideModerationException(Reason reason, Throwable cause) {
        super(reason == Reason.BLOCKED ? "content blocked by safety moderation" : "content safety verification unavailable", cause);
        this.reason = reason;
    }

    public Reason getReason() { return reason; }
}
