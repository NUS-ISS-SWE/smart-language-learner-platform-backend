package com.nus.iss.dto;

import java.time.LocalDate;

/** Aggregated system activity for one calendar day in UTC. */
public record DailyUsageMetric(
        LocalDate date,
        long newUsers,
        long compositionsCreated,
        long successfulReviews,
        long documentsUploaded,
        long aiTokensUsed,
        long estimatedAiCostMicros) {
}
