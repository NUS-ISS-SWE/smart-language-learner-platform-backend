package com.nus.iss.exception;

public class CompositionModeratedException extends RuntimeException {
    public CompositionModeratedException() {
        super("This composition has been removed by moderation and is not available for this action.");
    }
}
