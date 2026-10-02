package com.nus.iss.model;

import com.nus.iss.enums.GradingAttemptStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "grading_attempt",
        uniqueConstraints = @UniqueConstraint(name = "uq_grading_attempt_request_attempt",
                columnNames = {"request_id", "attempt_number"}),
        indexes = {
                @Index(name = "idx_grading_attempt_request_id", columnList = "request_id"),
                @Index(name = "idx_grading_attempt_status_completed", columnList = "status,completed_at")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradingAttempt {
    @Id
    private UUID id;
    @Column(name = "request_id", nullable = false, length = 255)
    private String requestId;
    @Column(name = "context_id", nullable = false, length = 255)
    private String contextId;
    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;
    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GradingAttemptStatus status;
    @Column(name = "composition_id")
    private Long compositionId;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "task_id", nullable = false, length = 36)
    private String taskId;
    @Column(name = "artifact_id", nullable = false, length = 36)
    private String artifactId;
    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    @Column(name = "model_version")
    private String modelVersion;
    @Column(name = "prompt_version")
    private String promptVersion;
    @Column(name = "prompt_tokens")
    private Long promptTokens;
    @Column(name = "completion_tokens")
    private Long completionTokens;
    @Column(name = "cached_input_tokens")
    private Long cachedInputTokens;
    @Column(name = "total_tokens")
    private Long totalTokens;
    @Column(name = "estimated_cost_micros")
    private Long estimatedCostMicros;
    @Column(name = "result_payload", columnDefinition = "TEXT")
    private String resultPayload;
    @Column(name = "error_code", length = 100)
    private String errorCode;
    @Column(name = "error_message", length = 500)
    private String errorMessage;
    @Version
    private long version;
}
