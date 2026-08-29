package com.nus.iss.dto;

import java.time.LocalDate;
import java.util.List;

/** Read-only metrics shown exclusively on the administrator dashboard. */
public record SystemUsageMetricsResponse(
        LocalDate from,
        LocalDate to,
        Summary summary,
        List<DailyUsageMetric> daily) {

    public record Summary(
            long totalUsers,
            long activeUsers,
            long newUsers,
            long compositionsCreated,
            long compositionsReviewed,
            long documentsUploaded,
            long aiTokensUsed) {
    }
}
