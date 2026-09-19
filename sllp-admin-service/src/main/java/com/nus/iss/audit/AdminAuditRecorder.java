package com.nus.iss.audit;

import java.util.UUID;

import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/** Records privileged actions without allowing an audit-store outage to block the product action. */
@Component
@Slf4j
public class AdminAuditRecorder {
    private final AuditEventService auditEvents;
    private final ObjectMapper objectMapper;

    public AdminAuditRecorder(AuditEventService auditEvents, ObjectMapper objectMapper) {
        this.auditEvents = auditEvents; this.objectMapper = objectMapper;
    }

    public void record(String action, String username, String userId, String targetType, String targetId,
            String requestId, String method, String path) {
        try {
            auditEvents.ingest(new AuditEventIngestRequest(null, null, "admin-service", "ADMIN", action, "SUCCESS",
                    "USER", userId, username, "ADMIN", targetType, targetId, parseUuid(requestId), null, null, null,
                    method, path, null, null, null, objectMapper.createObjectNode(), null));
        } catch (Exception error) {
            log.error("audit_write_failed action={} targetType={} targetId={}", action, targetType, targetId, error);
        }
    }

    private static UUID parseUuid(String value) {
        try { return value == null ? null : UUID.fromString(value); } catch (IllegalArgumentException ignored) { return null; }
    }
}
