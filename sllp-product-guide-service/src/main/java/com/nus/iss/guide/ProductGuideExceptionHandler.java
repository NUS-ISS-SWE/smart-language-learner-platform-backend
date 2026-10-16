package com.nus.iss.guide;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ProductGuideExceptionHandler {
    @ExceptionHandler(ProductGuideModerationException.class)
    ResponseEntity<Map<String, String>> moderation(ProductGuideModerationException error) {
        if (error.getReason() == ProductGuideModerationException.Reason.BLOCKED) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("code", "CONTENT_BLOCKED"));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("code", "SAFETY_UNAVAILABLE"));
    }
}
