package com.nus.iss.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneralException(Exception ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setProperty("code", "INTERNAL_ERROR");
        return problemDetail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("Bad Request");
        problemDetail.setProperty("code", "INVALID_REQUEST");
        return problemDetail;
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ProblemDetail handleNoSuchElementException(java.util.NoSuchElementException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Not Found");
        problemDetail.setProperty("code", "RESOURCE_NOT_FOUND");
        return problemDetail;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableRequest(HttpMessageNotReadableException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request body is missing or malformed");
        problemDetail.setTitle("Bad Request");
        problemDetail.setProperty("code", "INVALID_REQUEST");
        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        problemDetail.setTitle("Bad Request");
        problemDetail.setProperty("code", "VALIDATION_ERROR");
        problemDetail.setProperty("fields", ex.getBindingResult().getFieldErrors().stream()
                .collect(java.util.stream.Collectors.toMap(error -> error.getField(), error -> error.getDefaultMessage(), (first, ignored) -> first)));
        return problemDetail;
    }
}
