package com.nus.iss.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.nus.iss.dto.DailyUsageMetric;
import com.nus.iss.dto.SystemUsageMetricsResponse;
import com.nus.iss.dto.RetentionCohortMetric;

/** Read-model gateway for reporting queries; it deliberately does not expose domain entities. */
public interface SystemUsageMetricsQueryRepository {
    SystemUsageMetricsResponse.Summary getSummary(LocalDateTime start, LocalDateTime endExclusive);

    List<DailyUsageMetric> getDailyActivity(LocalDateTime start, LocalDateTime endExclusive);

    List<RetentionCohortMetric> getRetentionCohorts(LocalDateTime start, LocalDateTime endExclusive);

}
