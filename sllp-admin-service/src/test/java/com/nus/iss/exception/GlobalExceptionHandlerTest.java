package com.nus.iss.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void missingRubricIsNotFound() {
        ProblemDetail problem = handler.handleNotFound(new RubricCriterionNotFoundException(4L));

        assertEquals(HttpStatus.NOT_FOUND.value(), problem.getStatus());
        assertEquals("Rubric Criterion Not Found", problem.getTitle());
    }

    @Test
    void validationErrorsListTheFirstMessagePerField() {
        BeanPropertyBindingResult result = new BeanPropertyBindingResult(new Object(), "rubric");
        result.addError(new FieldError("rubric", "weight", "must be positive"));
        result.addError(new FieldError("rubric", "weight", "must be at most 100"));
        result.addError(new FieldError("rubric", "name", "must not be blank"));
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(result);

        ProblemDetail problem = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST.value(), problem.getStatus());
        assertEquals("Invalid Rubric Criterion", problem.getTitle());
        assertEquals(Map.of("weight", "must be positive", "name", "must not be blank"),
                problem.getProperties().get("errors"));
    }

    @Test
    void malformedRequestIsBadRequest() {
        ProblemDetail problem = handler.handleMalformedRequest(new IllegalStateException("bad json"));

        assertEquals(HttpStatus.BAD_REQUEST.value(), problem.getStatus());
        assertEquals("The request contains an invalid value.", problem.getDetail());
    }

    @Test
    void illegalArgumentKeepsItsMessage() {
        ProblemDetail problem = handler.handleIllegalArgument(new IllegalArgumentException("invalid audit outcome"));

        assertEquals(HttpStatus.BAD_REQUEST.value(), problem.getStatus());
        assertEquals("invalid audit outcome", problem.getDetail());
    }

    @Test
    void constraintViolationIsConflict() {
        ProblemDetail problem = handler.handleDataIntegrity(new DataIntegrityViolationException("duplicate key"));

        assertEquals(HttpStatus.CONFLICT.value(), problem.getStatus());
    }

    @Test
    void anythingElseIsAnInternalErrorWithoutDetails() {
        ProblemDetail problem = handler.handleGeneric(new RuntimeException("NullPointer in SQL layer"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), problem.getStatus());
        assertEquals("An unexpected error occurred.", problem.getDetail());
    }
}
