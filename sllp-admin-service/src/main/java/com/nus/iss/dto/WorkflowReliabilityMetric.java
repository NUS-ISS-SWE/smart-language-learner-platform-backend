package com.nus.iss.dto;

/** Terminal assessment-workflow outcomes and completion latency for the selected period. */
public record WorkflowReliabilityMetric(
        long received,
        long completed,
        long failed,
        long pending,
        Double completionPercent,
        Double failurePercent,
        Long averageCompletionLatencyMillis) {
}
