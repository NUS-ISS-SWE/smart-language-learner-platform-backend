package com.nus.iss.exception;

public class PromptInjectionDetectedException extends RuntimeException {
    public PromptInjectionDetectedException() {
        super("Lakera Guard detected unsafe grading content");
    }
}
