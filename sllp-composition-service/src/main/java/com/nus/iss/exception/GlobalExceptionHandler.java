package com.nus.iss.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.server.ResponseStatusException;

import lombok.extern.slf4j.Slf4j;

import java.util.NoSuchElementException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AssessmentQuotaExceededException.class)
    public ProblemDetail handleQuotaExceeded(AssessmentQuotaExceededException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        problemDetail.setTitle("Assessment quota exhausted");
        problemDetail.setProperty("code", "ASSESSMENT_QUOTA_EXHAUSTED");
        return problemDetail;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatusException(ResponseStatusException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(ex.getStatusCode(),
                ex.getReason() == null ? "Request failed" : ex.getReason());
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        problemDetail.setTitle(status == null ? "Request failed" : status.getReasonPhrase());
        problemDetail.setProperty("code",
                ex.getStatusCode().value() == HttpStatus.SERVICE_UNAVAILABLE.value()
                        ? "UPSTREAM_SERVICE_UNAVAILABLE"
                        : "REQUEST_FAILED");
        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneralException(Exception ex) {
        log.error("Unhandled exception in composition service", ex);
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

    @ExceptionHandler({CompositionModeratedException.class, ModerationConflictException.class})
    public ProblemDetail handleModerationConflict(RuntimeException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Moderation conflict");
        problemDetail.setProperty("code", "MODERATION_CONFLICT");
        return problemDetail;
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleConcurrentModeration(ObjectOptimisticLockingFailureException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "This composition was changed by another administrator. Refresh and try again.");
        problemDetail.setTitle("Moderation conflict");
        problemDetail.setProperty("code", "MODERATION_CONFLICT");
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
