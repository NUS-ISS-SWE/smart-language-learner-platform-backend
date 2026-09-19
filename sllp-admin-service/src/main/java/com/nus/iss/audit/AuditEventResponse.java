package com.nus.iss.audit;

import java.time.Instant;
import java.util.UUID;

import tools.jackson.databind.JsonNode;

public record AuditEventResponse(UUID id, Instant occurredAt, String service, String category,
        String action, String outcome, String actorType, String actorId, String actorName,
        String actorRole, String targetType, String targetId, UUID requestId,
        String correlationId, String sourceIp, String userAgent, String httpMethod,
        String httpPath, Integer httpStatus, JsonNode beforeData, JsonNode afterData,
        JsonNode metadata, String errorCode) {
    static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(event.getId(), event.getOccurredAt(), event.getService(), event.getCategory(),
                event.getAction(), event.getOutcome(), event.getActorType(), event.getActorId(), event.getActorName(),
                event.getActorRole(), event.getTargetType(), event.getTargetId(), event.getRequestId(),
                event.getCorrelationId(), event.getSourceIp(), event.getUserAgent(), event.getHttpMethod(),
                event.getHttpPath(), event.getHttpStatus(), event.getBeforeData(), event.getAfterData(),
                event.getMetadata(), event.getErrorCode());
    }
}
