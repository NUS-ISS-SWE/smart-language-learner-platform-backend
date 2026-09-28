package com.nus.iss.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.nus.iss.contracts.audit.AuditEventPublisher;
import com.nus.iss.enums.ModerationAction;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

/** Emits a concise event without copying composition text or moderator notes. */
@Service
@Slf4j
public class CompositionModerationAuditService {
    private final AuditEventPublisher publisher;

    public CompositionModerationAuditService(ObjectMapper mapper,
            @Value("${sllp.audit.ingest-url:http://localhost:8086/internal/audit-events}") String url,
            @Value("${sllp.audit.ingest-secret:}") String secret) {
        publisher = new AuditEventPublisher("composition-service", url, secret, mapper);
    }

    public void record(ModerationAction action, Long moderatorId, String moderatorName, Long compositionId,
            String requestId, String path) {
        publisher.publish("ADMIN", action == ModerationAction.REMOVE ? "COMPOSITION_REMOVED" : "COMPOSITION_RESTORED",
                "SUCCESS", "USER", String.valueOf(moderatorId), moderatorName, "ADMIN", "COMPOSITION",
                String.valueOf(compositionId), requestId, null, null, "PATCH", path, 200,
                error -> log.error("audit_write_failed action={} compositionId={}", action, compositionId, error));
    }
}
