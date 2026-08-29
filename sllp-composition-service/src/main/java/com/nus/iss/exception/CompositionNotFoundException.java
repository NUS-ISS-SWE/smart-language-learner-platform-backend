package com.nus.iss.exception;

/** Raised when a composition is absent or is not owned by the requesting user. */
public class CompositionNotFoundException extends java.util.NoSuchElementException {
    public CompositionNotFoundException() {
        super("Composition not found");
    }
}
