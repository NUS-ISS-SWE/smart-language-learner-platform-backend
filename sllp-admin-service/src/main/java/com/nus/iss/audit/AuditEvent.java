package com.nus.iss.audit;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import tools.jackson.databind.JsonNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "audit_event")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {
    @Id private UUID id;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
    @Column(nullable = false) private String service;
    @Column(nullable = false) private String category;
    @Column(nullable = false) private String action;
    @Column(nullable = false) private String outcome;
    @Column(name = "actor_type", nullable = false) private String actorType;
    @Column(name = "actor_id") private String actorId;
    @Column(name = "actor_name") private String actorName;
    @Column(name = "actor_role") private String actorRole;
    @Column(name = "target_type") private String targetType;
    @Column(name = "target_id") private String targetId;
    @Column(name = "request_id") private UUID requestId;
    @Column(name = "correlation_id") private String correlationId;
    @Column(name = "source_ip") private String sourceIp;
    @Column(name = "user_agent") private String userAgent;
    @Column(name = "http_method") private String httpMethod;
    @Column(name = "http_path") private String httpPath;
    @Column(name = "http_status") private Integer httpStatus;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "before_data", columnDefinition = "jsonb") private JsonNode beforeData;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "after_data", columnDefinition = "jsonb") private JsonNode afterData;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb") private JsonNode metadata;
    @Column(name = "error_code") private String errorCode;
}
