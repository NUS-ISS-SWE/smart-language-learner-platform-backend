package com.nus.iss.exception;

import com.nus.iss.controller.GradingA2aController;
import com.nus.iss.dto.A2aResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;

@Slf4j
@RestControllerAdvice(assignableTypes = GradingA2aController.class)
public class A2aExceptionHandler {
    @ExceptionHandler(A2aUnauthorizedException.class)
    public ResponseEntity<Void> unauthorized() {
        log.warn("Rejected unauthorized A2A grading request");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @ExceptionHandler({A2aInvalidRequestException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<A2aResponseDTO> invalidRequest(Exception error) {
        String requestId = error instanceof A2aInvalidRequestException invalid
                ? invalid.getRequestId() : UUID.randomUUID().toString();
        log.warn("Rejected invalid A2A grading request requestId={} category={}", requestId, error.getClass().getSimpleName());
        return ResponseEntity.ok(A2aResponseDTO.error(requestId, -32602, "invalid A2A grading request"));
    }

    @ExceptionHandler(A2aExecutionException.class)
    public ResponseEntity<A2aResponseDTO> executionFailure(A2aExecutionException error) {
        log.warn("A2A grading failed requestId={} category={}", error.getRequestId(),
                error.getCause() == null ? error.getClass().getSimpleName() : error.getCause().getClass().getSimpleName());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(A2aResponseDTO.error(error.getRequestId(), -32603, "grading execution failed"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<A2aResponseDTO> unexpectedFailure(Exception error) {
        String requestId = UUID.randomUUID().toString();
        log.error("Unexpected A2A grading failure requestId={} category={}", requestId,
                error.getClass().getSimpleName(), error);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(A2aResponseDTO.error(requestId, -32603, "grading execution failed"));
    }
}
