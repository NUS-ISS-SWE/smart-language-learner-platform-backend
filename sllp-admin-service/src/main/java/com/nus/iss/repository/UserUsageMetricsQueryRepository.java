package com.nus.iss.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.nus.iss.dto.DailyUsageMetric;
import com.nus.iss.dto.UserUsageMetricsResponse;

/** Read-model gateway for a single learner's usage history. */
public interface UserUsageMetricsQueryRepository {
    UserUsageMetricsResponse.Summary getUserSummary(long userId, LocalDateTime start, LocalDateTime endExclusive);

    List<DailyUsageMetric> getUserDailyActivity(long userId, LocalDateTime start, LocalDateTime endExclusive);
}
