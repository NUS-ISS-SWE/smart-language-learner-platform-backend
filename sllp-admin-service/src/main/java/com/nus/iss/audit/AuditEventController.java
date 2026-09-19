package com.nus.iss.audit;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;

@RestController
public class AuditEventController {
    private final AuditEventService service;
    private final AuditSignatureVerifier signatures;
    private final ObjectMapper objectMapper;

    public AuditEventController(AuditEventService service, AuditSignatureVerifier signatures, ObjectMapper objectMapper) {
        this.service = service; this.signatures = signatures; this.objectMapper = objectMapper;
    }

    @PostMapping(path = "/internal/audit-events", consumes = "application/json")
    public ResponseEntity<AuditEventResponse> ingest(@RequestHeader("X-Sllp-Audit-Timestamp") String timestamp,
            @RequestHeader("X-Sllp-Audit-Signature") String signature, @RequestBody String body) throws Exception {
        signatures.verify(timestamp, signature, body);
        AuditEventIngestRequest request = objectMapper.readValue(body, AuditEventIngestRequest.class);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.ingest(request));
    }

    @GetMapping("/api/v1/admin/audit-events")
    public Page<AuditEventResponse> search(@RequestHeader("X-Sllp-Role") String role,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String actorId, @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String targetId, @RequestParam(required = false) String action,
            @RequestParam(required = false) String category, @RequestParam(required = false) String outcome,
            @RequestParam(required = false) UUID requestId, @RequestParam(required = false) String correlationId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {
        if (!"ADMIN".equals(role)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        return service.search(from, to, actorId, targetType, targetId, action, category, outcome, requestId, correlationId, page, size);
    }
}
