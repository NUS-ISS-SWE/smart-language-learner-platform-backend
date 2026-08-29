package com.nus.iss.exception;

/**
 * Signals invalid composition command, query, or event data.
 *
 * <p>It remains an {@link IllegalArgumentException} so the API preserves its
 * existing 400 response behaviour while callers no longer need to infer the
 * domain from a generic exception type.</p>
 */
public class InvalidCompositionRequestException extends IllegalArgumentException {
    public InvalidCompositionRequestException(String message) {
        super(message);
    }

    public InvalidCompositionRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
