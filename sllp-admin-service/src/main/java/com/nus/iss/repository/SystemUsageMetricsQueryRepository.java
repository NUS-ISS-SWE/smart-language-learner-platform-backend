package com.nus.iss.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.nus.iss.dto.DailyUsageMetric;
import com.nus.iss.dto.SystemUsageMetricsResponse;
import com.nus.iss.dto.RetentionCohortMetric;
import com.nus.iss.dto.UserUsageMetricsResponse;

/** Read-model gateway for reporting queries; it deliberately does not expose domain entities. */
public interface SystemUsageMetricsQueryRepository {
    SystemUsageMetricsResponse.Summary getSummary(LocalDateTime start, LocalDateTime endExclusive);

    List<DailyUsageMetric> getDailyActivity(LocalDateTime start, LocalDateTime endExclusive);

    List<RetentionCohortMetric> getRetentionCohorts(LocalDateTime start, LocalDateTime endExclusive);

    UserUsageMetricsResponse.Summary getUserSummary(long userId, LocalDateTime start, LocalDateTime endExclusive);

    List<DailyUsageMetric> getUserDailyActivity(long userId, LocalDateTime start, LocalDateTime endExclusive);
}
