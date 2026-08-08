package com.nus.iss.exception;

public class RubricCriterionNotFoundException extends RuntimeException {

    public RubricCriterionNotFoundException(Long id) {
        super("Rubric criterion with ID " + id + " was not found");
    }
}
