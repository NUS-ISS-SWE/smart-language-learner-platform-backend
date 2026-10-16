package com.nus.iss.service.impl;

import com.nus.iss.contracts.audit.AuditEventPublisher;
import com.nus.iss.dto.GradingRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

/** Best-effort metadata-only audit trail; audit delivery never weakens fail-closed moderation. */
@Component
@Slf4j
class ModerationAuditPublisher {
    private final AuditEventPublisher publisher;

    ModerationAuditPublisher(ObjectMapper mapper,
            @Value("${sllp.audit.ingest-url:http://localhost:8086/internal/audit-events}") String ingestUrl,
            @Value("${sllp.audit.ingest-secret:}") String ingestSecret) {
        publisher = new AuditEventPublisher("grading-agent", ingestUrl, ingestSecret, mapper);
    }

    void publish(GradingRequestDTO request, String stage, String outcome, String model, double threshold) {
        publisher.publish("AI_INVOCATION", "MODERATION_CHECK", outcome, "USER", request.getUserId() == null ? null : Long.toString(request.getUserId()), null,
                null, "COMPOSITION", request.getCompositionId() == null ? null : Long.toString(request.getCompositionId()), request.getRequestId(), null, null,
                null, null, 0, Map.of("operation", "MODERATION_CHECK", "stage", stage, "decision", outcome,
                        "provider", "OPENAI", "model", model == null ? "" : model,
                        "threshold", Double.toString(threshold)),
                error -> log.warn("Moderation audit write failed category={}", error.getClass().getSimpleName()));
    }
}
