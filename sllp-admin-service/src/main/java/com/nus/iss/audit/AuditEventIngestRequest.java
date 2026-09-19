package com.nus.iss.audit;

import java.time.Instant;
import java.util.UUID;

import tools.jackson.databind.JsonNode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AuditEventIngestRequest(
        UUID id, Instant occurredAt, @NotBlank String service, @NotBlank String category,
        @NotBlank String action, @NotBlank String outcome, @NotBlank String actorType,
        String actorId, String actorName, String actorRole, String targetType, String targetId,
        UUID requestId, String correlationId, String sourceIp, String userAgent,
        String httpMethod, String httpPath, Integer httpStatus, JsonNode beforeData,
        JsonNode afterData, JsonNode metadata, String errorCode) {
}
