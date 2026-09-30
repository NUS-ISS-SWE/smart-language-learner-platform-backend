package com.nus.iss.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(NoSuchElementException.class) ProblemDetail notFound(NoSuchElementException error) { return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, error.getMessage()); }
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class}) ProblemDetail invalid(Exception error) { return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, error instanceof MethodArgumentNotValidException ? "One or more fields are invalid." : error.getMessage()); }
    @ExceptionHandler(IllegalStateException.class) ProblemDetail conflict(IllegalStateException error) { return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, error.getMessage()); }
}
