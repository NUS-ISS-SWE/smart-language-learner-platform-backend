package com.nus.iss.dto;

import java.time.LocalDate;
import java.util.List;

/** Read-only system metrics exposed to administrators. */
public record SystemUsageMetricsResponse(
        LocalDate from,
        LocalDate to,
        Summary summary,
        List<DailyUsageMetric> daily,
        List<RetentionCohortMetric> retentionCohorts,
        WorkflowReliabilityMetric workflowReliability,
        List<AgentMetrics> agentMetrics) {

    public record Summary(
            long totalUsers,
            long activeUsers,
            long newUsers,
            long compositionsCreated,
            long activeLearners,
            long reviewRequests,
            long successfulReviews,
            long failedReviews,
            Double reviewSuccessPercent,
            Double reviewFailurePercent,
            Long averageReviewLatencyMillis,
            long documentsUploaded,
            long documentsProcessed,
            long documentsReady,
            long documentsFailed,
            Double documentSuccessPercent,
            Double documentFailurePercent,
            long totalAiTokensUsed,
            long aiTokensUsed,
            long estimatedAiCostMicros) {
    }
}
