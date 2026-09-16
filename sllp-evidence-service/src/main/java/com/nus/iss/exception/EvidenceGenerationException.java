package com.nus.iss.exception;

/** The evidence agent itself failed — bad/missing model output, or the model call errored. */
public class EvidenceGenerationException extends RuntimeException {
    public EvidenceGenerationException(String message) {
        super(message);
    }

    public EvidenceGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
