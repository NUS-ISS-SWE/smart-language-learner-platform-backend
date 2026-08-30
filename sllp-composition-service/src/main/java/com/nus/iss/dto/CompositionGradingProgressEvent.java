package com.nus.iss.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Student-safe progress update emitted by the assessment orchestrator. */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CompositionGradingProgressEvent {
    private String schemaVersion;
    private String workflowId;
    private Long compositionId;
    private Long userId;
    private String stage;
    private String message;
    private LocalDateTime updatedAt;
}
