package com.nus.iss.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    void handleGeneralExceptionReturnsInternalServerErrorProblemDetail() {
        ProblemDetail problemDetail = exceptionHandler.handleGeneralException(new RuntimeException("Unexpected"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), problemDetail.getStatus());
        assertEquals("Internal Server Error", problemDetail.getTitle());
        assertEquals("Unexpected", problemDetail.getDetail());
    }

    @Test
    void handleIllegalArgumentExceptionReturnsBadRequestProblemDetail() {
        ProblemDetail problemDetail =
                exceptionHandler.handleIllegalArgumentException(new IllegalArgumentException("Bad request"));

        assertEquals(HttpStatus.BAD_REQUEST.value(), problemDetail.getStatus());
        assertEquals("Bad Request", problemDetail.getTitle());
        assertEquals("Bad request", problemDetail.getDetail());
    }

    @Test
    void handleNoSuchElementExceptionReturnsNotFoundProblemDetail() {
        ProblemDetail problemDetail =
                exceptionHandler.handleNoSuchElementException(new NoSuchElementException("Composition not found"));

        assertEquals(HttpStatus.NOT_FOUND.value(), problemDetail.getStatus());
        assertEquals("Not Found", problemDetail.getTitle());
        assertEquals("Composition not found", problemDetail.getDetail());
    }
}
