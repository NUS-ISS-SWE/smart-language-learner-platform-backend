package com.nus.iss.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Usage and reliability measures for one learner, visible only to administrators. */
public record UserUsageMetricsResponse(
        long userId,
        LocalDate from,
        LocalDate to,
        Summary summary,
        List<DailyUsageMetric> daily) {

    public record Summary(
            long activeDays,
            long compositionsCreated,
            long reviewRequests,
            long successfulReviews,
            long failedReviews,
            long documentsUploaded,
            long documentsReady,
            long documentsFailed,
            long aiTokensUsed,
            long estimatedAiCostMicros,
            Instant lastActivityAt) {
    }
}
