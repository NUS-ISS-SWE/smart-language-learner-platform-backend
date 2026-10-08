package com.nus.iss.controller;

import com.nus.iss.dto.AssessmentQuotaReservationRequest;
import com.nus.iss.dto.AssessmentQuotaReservationResponse;
import com.nus.iss.service.AssessmentQuotaService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/internal/assessment-quotas") @RequiredArgsConstructor
public class InternalAssessmentQuotaController {
    private final AssessmentQuotaService service;
    @Value("${sllp.internal-service-token:}") private String token;

    @PostMapping("/reservations")
    public ResponseEntity<AssessmentQuotaReservationResponse> reserve(
            @RequestHeader(value = "X-Sllp-Internal-Token", required = false) String provided,
            @Valid @RequestBody AssessmentQuotaReservationRequest request) {
        requireAuthorized(provided);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.reserve(request.userId(), request.requestId()));
    }

    @PostMapping("/reservations/{requestId}/settlement")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void settle(@RequestHeader(value = "X-Sllp-Internal-Token", required = false) String provided,
                       @PathVariable java.util.UUID requestId, @RequestParam boolean consumed) {
        requireAuthorized(provided);
        service.settle(requestId, consumed);
    }

    private void requireAuthorized(String provided) {
        if (token == null || token.isBlank() || provided == null || !MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8))) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "unauthorized");
        }
    }
}
