package com.nus.iss.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.nus.iss.dto.DailyUsageMetric;
import com.nus.iss.dto.UserUsageMetricsResponse;

import lombok.RequiredArgsConstructor;

/** PostgreSQL read model for an individual learner's usage history. */
@Repository
@RequiredArgsConstructor
public class JdbcUserUsageMetricsQueryRepository implements UserUsageMetricsQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final String USER_DAILY_ACTIVITY_SQL = """
            WITH dates AS (SELECT generate_series(CAST(:start AS date), CAST(:endExclusive AS date) - INTERVAL '1 day', INTERVAL '1 day')::date AS metric_date),
            users_by_day AS (SELECT created_at::date AS metric_date, COUNT(*) AS value FROM app_user WHERE deleted_at IS NULL AND id = :userId AND created_at >= :start AND created_at < :endExclusive GROUP BY created_at::date),
            compositions_by_day AS (SELECT created_at::date AS metric_date, COUNT(*) AS value FROM composition WHERE user_id = :userId AND created_at >= :start AND created_at < :endExclusive GROUP BY created_at::date),
            reviews_by_day AS (SELECT e.evaluated_at::date AS metric_date, COUNT(*) AS value FROM evaluation e JOIN composition c ON c.id = e.composition_id WHERE c.user_id = :userId AND e.success = TRUE AND e.evaluated_at >= :start AND e.evaluated_at < :endExclusive GROUP BY e.evaluated_at::date),
            documents_by_day AS (SELECT created_at::date AS metric_date, COUNT(*) AS value FROM documents WHERE user_id = :userId AND created_at >= :start AND created_at < :endExclusive GROUP BY created_at::date),
            ai_by_day AS (SELECT created_at::date AS metric_date, SUM(total_tokens) AS tokens, SUM(estimated_cost_micros) AS cost FROM ai_usage_event WHERE user_id = :userId AND created_at >= :start AND created_at < :endExclusive GROUP BY created_at::date)
            SELECT dates.metric_date, COALESCE(users_by_day.value, 0) AS new_users, COALESCE(compositions_by_day.value, 0) AS compositions_created,
                COALESCE(reviews_by_day.value, 0) AS successful_reviews, COALESCE(documents_by_day.value, 0) AS documents_uploaded,
                COALESCE(ai_by_day.tokens, 0) AS ai_tokens_used, COALESCE(ai_by_day.cost, 0) AS estimated_ai_cost_micros
            FROM dates LEFT JOIN users_by_day USING (metric_date) LEFT JOIN compositions_by_day USING (metric_date)
                LEFT JOIN reviews_by_day USING (metric_date) LEFT JOIN documents_by_day USING (metric_date) LEFT JOIN ai_by_day USING (metric_date)
            ORDER BY dates.metric_date
            """;

    @Override
    public UserUsageMetricsResponse.Summary getUserSummary(long userId, LocalDateTime start, LocalDateTime endExclusive) {
        String sql = """
                WITH activity AS (
                    SELECT created_at AS activity_at FROM composition WHERE user_id = :userId AND created_at >= :start AND created_at < :endExclusive
                    UNION ALL SELECT e.evaluated_at FROM evaluation e JOIN composition c ON c.id = e.composition_id WHERE c.user_id = :userId AND e.evaluated_at >= :start AND e.evaluated_at < :endExclusive
                    UNION ALL SELECT created_at FROM documents WHERE user_id = :userId AND created_at >= :start AND created_at < :endExclusive
                    UNION ALL SELECT created_at AT TIME ZONE 'UTC' FROM agent_workflow_run WHERE user_id = :userId AND created_at >= (:start AT TIME ZONE 'UTC') AND created_at < (:endExclusive AT TIME ZONE 'UTC')
                ), review_requests AS (
                    SELECT source_event_id AS request_key FROM agent_workflow_run WHERE user_id = :userId AND created_at >= (:start AT TIME ZONE 'UTC') AND created_at < (:endExclusive AT TIME ZONE 'UTC')
                    UNION
                    SELECT COALESCE(e.request_id::text, 'legacy:' || e.composition_id || ':' || e.evaluated_at::text)
                    FROM evaluation e JOIN composition c ON c.id = e.composition_id
                    WHERE c.user_id = :userId AND e.requested_at >= :start AND e.requested_at < :endExclusive
                      AND (e.request_id IS NULL OR NOT EXISTS (SELECT 1 FROM agent_workflow_run w WHERE w.source_event_id = e.request_id::text))
                )
                SELECT
                    (SELECT COUNT(DISTINCT activity_at::date) FROM activity) AS active_days,
                    (SELECT COUNT(*) FROM composition WHERE user_id = :userId AND created_at >= :start AND created_at < :endExclusive) AS compositions_created,
                    (SELECT COUNT(*) FROM review_requests) AS review_requests,
                    (SELECT COUNT(*) FROM evaluation e JOIN composition c ON c.id = e.composition_id WHERE c.user_id = :userId AND e.success = TRUE AND e.evaluated_at >= :start AND e.evaluated_at < :endExclusive) AS successful_reviews,
                    (SELECT COUNT(*) FROM evaluation e JOIN composition c ON c.id = e.composition_id WHERE c.user_id = :userId AND e.success = FALSE AND e.evaluated_at >= :start AND e.evaluated_at < :endExclusive) AS failed_reviews,
                    (SELECT COUNT(*) FROM documents WHERE user_id = :userId AND created_at >= :start AND created_at < :endExclusive) AS documents_uploaded,
                    (SELECT COUNT(*) FROM documents WHERE user_id = :userId AND status = 'READY' AND processed_at >= :start AND processed_at < :endExclusive) AS documents_ready,
                    (SELECT COUNT(*) FROM documents WHERE user_id = :userId AND status = 'FAILED' AND processed_at >= :start AND processed_at < :endExclusive) AS documents_failed,
                    (SELECT COALESCE(SUM(total_tokens), 0) FROM ai_usage_event WHERE user_id = :userId AND created_at >= :start AND created_at < :endExclusive) AS ai_tokens_used,
                    (SELECT COALESCE(SUM(estimated_cost_micros), 0) FROM ai_usage_event WHERE user_id = :userId AND created_at >= :start AND created_at < :endExclusive) AS estimated_ai_cost_micros,
                    (SELECT MAX(activity_at) FROM activity) AS last_activity_at
                """;
        return jdbcTemplate.queryForObject(sql, parameters(userId, start, endExclusive), (rs, rowNum) -> {
            Timestamp lastActivity = rs.getTimestamp("last_activity_at");
            Instant instant = lastActivity == null ? null : lastActivity.toLocalDateTime().toInstant(ZoneOffset.UTC);
            return new UserUsageMetricsResponse.Summary(rs.getLong("active_days"), rs.getLong("compositions_created"),
                    rs.getLong("review_requests"), rs.getLong("successful_reviews"), rs.getLong("failed_reviews"),
                    rs.getLong("documents_uploaded"), rs.getLong("documents_ready"), rs.getLong("documents_failed"),
                    rs.getLong("ai_tokens_used"), rs.getLong("estimated_ai_cost_micros"), instant);
        });
    }

    @Override
    public List<DailyUsageMetric> getUserDailyActivity(long userId, LocalDateTime start, LocalDateTime endExclusive) {
        return jdbcTemplate.query(USER_DAILY_ACTIVITY_SQL, parameters(userId, start, endExclusive), (rs, rowNum) -> new DailyUsageMetric(
                rs.getDate("metric_date").toLocalDate(), rs.getLong("new_users"), rs.getLong("compositions_created"),
                rs.getLong("successful_reviews"), rs.getLong("documents_uploaded"), rs.getLong("ai_tokens_used"), rs.getLong("estimated_ai_cost_micros")));
    }

    private static MapSqlParameterSource parameters(long userId, LocalDateTime start, LocalDateTime endExclusive) {
        return new MapSqlParameterSource().addValue("start", start).addValue("endExclusive", endExclusive).addValue("userId", userId);
    }
}
