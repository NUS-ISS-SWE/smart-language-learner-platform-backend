package com.nus.iss.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.nus.iss.dto.CompositionResponse;
import com.nus.iss.dto.ModerationRequest;
import com.nus.iss.service.CompositionModerationAuditService;
import com.nus.iss.service.CompositionModerationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/compositions")
@RequiredArgsConstructor
public class AdminCompositionModerationController {
    private final CompositionModerationService moderationService;
    private final CompositionModerationAuditService auditService;

    @PatchMapping("/{id}/moderation")
    public ResponseEntity<CompositionResponse> moderate(
            @RequestHeader("X-Sllp-Role") String role,
            @RequestHeader("X-Sllp-User-Id") Long moderatorId,
            @RequestHeader(value = "X-Sllp-Username", required = false) String moderatorName,
            @RequestHeader(value = "X-Sllp-Request-Id", required = false) String requestId,
            @PathVariable Long id,
            @RequestBody @Valid ModerationRequest request) {
        if (!"ADMIN".equals(role)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        CompositionResponse response = moderationService.moderate(id, moderatorId, moderatorName, request);
        auditService.record(request.action(), moderatorId, moderatorName, id, requestId,
                "/api/v1/admin/compositions/" + id + "/moderation");
        return ResponseEntity.ok(response);
    }
}
