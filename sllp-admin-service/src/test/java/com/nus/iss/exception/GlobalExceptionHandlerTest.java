package com.nus.iss.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private MethodArgumentNotValidException validationException;
    @Mock
    private BindingResult bindingResult;

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void notFoundReturns404() {
        ProblemDetail result = handler.handleNotFound(new RubricCriterionNotFoundException(42L));

        assertEquals(HttpStatus.NOT_FOUND.value(), result.getStatus());
        assertEquals("Rubric Criterion Not Found", result.getTitle());
        assertEquals("Rubric criterion with ID 42 was not found", result.getDetail());
    }

    @Test
    void validationReturnsFirstMessagePerField() {
        when(validationException.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("request", "criterionName", "must not be blank"),
                new FieldError("request", "criterionName", "must be shorter"),
                new FieldError("request", "weightPercentage", "must be positive")));

        ProblemDetail result = handler.handleValidation(validationException);

        assertEquals(HttpStatus.BAD_REQUEST.value(), result.getStatus());
        assertEquals("Invalid Rubric Criterion", result.getTitle());
        assertEquals(Map.of("criterionName", "must not be blank", "weightPercentage", "must be positive"),
                result.getProperties().get("errors"));
    }

    @Test
    void malformedAndIllegalArgumentsReturn400() {
        ProblemDetail malformed = handler.handleMalformedRequest(
                org.mockito.Mockito.mock(HttpMessageNotReadableException.class));
        ProblemDetail illegal = handler.handleIllegalArgument(new IllegalArgumentException("bad range"));

        assertEquals(HttpStatus.BAD_REQUEST.value(), malformed.getStatus());
        assertEquals("The request contains an invalid value.", malformed.getDetail());
        assertEquals(HttpStatus.BAD_REQUEST.value(), illegal.getStatus());
        assertEquals("bad range", illegal.getDetail());
    }

    @Test
    void persistenceConflictReturnsSanitized409() {
        ProblemDetail result = handler.handleDataIntegrity(
                new DataIntegrityViolationException("sensitive database detail"));

        assertEquals(HttpStatus.CONFLICT.value(), result.getStatus());
        assertEquals("Rubric Criterion Conflict", result.getTitle());
        assertEquals("The rubric criterion conflicts with a database constraint.", result.getDetail());
    }

    @Test
    void genericFailureReturnsSanitized500() {
        ProblemDetail result = handler.handleGeneric(new RuntimeException("sensitive detail"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), result.getStatus());
        assertEquals("Internal Server Error", result.getTitle());
        assertEquals("An unexpected error occurred.", result.getDetail());
    }
}
