package com.nus.iss.dto;

import java.time.LocalDate;

/** Aggregated system activity for one calendar day (UTC). */
public record DailyUsageMetric(
        LocalDate date,
        long newUsers,
        long compositionsCreated,
        long compositionsReviewed,
        long documentsUploaded) {
}
