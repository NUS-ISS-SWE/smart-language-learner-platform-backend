package com.nus.iss.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.nus.iss.dto.DailyUsageMetric;
import com.nus.iss.dto.RetentionCohortMetric;
import com.nus.iss.dto.SystemUsageMetricsResponse;
import com.nus.iss.dto.UserUsageMetricsResponse;

import lombok.RequiredArgsConstructor;

/** PostgreSQL reporting read model for cross-service administrator metrics. */
@Repository
@RequiredArgsConstructor
public class JdbcSystemUsageMetricsQueryRepository implements SystemUsageMetricsQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final String SYSTEM_DAILY_ACTIVITY_SQL = """
            WITH dates AS (SELECT generate_series(CAST(:start AS date), CAST(:endExclusive AS date) - INTERVAL '1 day', INTERVAL '1 day')::date AS metric_date),
            users_by_day AS (SELECT created_at::date AS metric_date, COUNT(*) AS value FROM app_user WHERE deleted_at IS NULL AND created_at >= :start AND created_at < :endExclusive GROUP BY created_at::date),
            compositions_by_day AS (SELECT created_at::date AS metric_date, COUNT(*) AS value FROM composition WHERE created_at >= :start AND created_at < :endExclusive GROUP BY created_at::date),
            reviews_by_day AS (SELECT e.evaluated_at::date AS metric_date, COUNT(*) AS value FROM evaluation e WHERE e.success = TRUE AND e.evaluated_at >= :start AND e.evaluated_at < :endExclusive GROUP BY e.evaluated_at::date),
            documents_by_day AS (SELECT created_at::date AS metric_date, COUNT(*) AS value FROM documents WHERE created_at >= :start AND created_at < :endExclusive GROUP BY created_at::date),
            ai_by_day AS (SELECT created_at::date AS metric_date, SUM(total_tokens) AS tokens, SUM(estimated_cost_micros) AS cost FROM ai_usage_event WHERE created_at >= :start AND created_at < :endExclusive GROUP BY created_at::date)
            SELECT dates.metric_date, COALESCE(users_by_day.value, 0) AS new_users, COALESCE(compositions_by_day.value, 0) AS compositions_created,
                COALESCE(reviews_by_day.value, 0) AS successful_reviews, COALESCE(documents_by_day.value, 0) AS documents_uploaded,
                COALESCE(ai_by_day.tokens, 0) AS ai_tokens_used, COALESCE(ai_by_day.cost, 0) AS estimated_ai_cost_micros
            FROM dates LEFT JOIN users_by_day USING (metric_date) LEFT JOIN compositions_by_day USING (metric_date)
                LEFT JOIN reviews_by_day USING (metric_date) LEFT JOIN documents_by_day USING (metric_date) LEFT JOIN ai_by_day USING (metric_date)
            ORDER BY dates.metric_date
            """;

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
    public SystemUsageMetricsResponse.Summary getSummary(LocalDateTime start, LocalDateTime endExclusive) {
        String sql = """
                WITH period_activity AS (
                    SELECT user_id FROM composition WHERE created_at >= :start AND created_at < :endExclusive
                    UNION SELECT c.user_id FROM evaluation e JOIN composition c ON c.id = e.composition_id WHERE e.evaluated_at >= :start AND e.evaluated_at < :endExclusive
                    UNION SELECT user_id FROM documents WHERE created_at >= :start AND created_at < :endExclusive
                )
                SELECT
                    (SELECT COUNT(*) FROM app_user WHERE deleted_at IS NULL) AS total_users,
                    (SELECT COUNT(*) FROM app_user WHERE deleted_at IS NULL AND status = 'ACTIVE') AS active_users,
                    (SELECT COUNT(*) FROM app_user WHERE deleted_at IS NULL AND created_at >= :start AND created_at < :endExclusive) AS new_users,
                    (SELECT COUNT(*) FROM composition WHERE created_at >= :start AND created_at < :endExclusive) AS compositions_created,
                    (SELECT COUNT(*) FROM period_activity) AS active_learners,
                    (SELECT COUNT(*) FROM evaluation WHERE requested_at >= :start AND requested_at < :endExclusive) AS review_requests,
                    (SELECT COUNT(*) FROM evaluation WHERE success = TRUE AND evaluated_at >= :start AND evaluated_at < :endExclusive) AS successful_reviews,
                    (SELECT COUNT(*) FROM evaluation WHERE success = FALSE AND evaluated_at >= :start AND evaluated_at < :endExclusive) AS failed_reviews,
                    (SELECT ROUND(AVG(EXTRACT(EPOCH FROM (evaluated_at - requested_at)) * 1000)) FROM evaluation WHERE requested_at IS NOT NULL AND evaluated_at >= :start AND evaluated_at < :endExclusive) AS average_review_latency_millis,
                    (SELECT COUNT(*) FROM documents WHERE created_at >= :start AND created_at < :endExclusive) AS documents_uploaded,
                    (SELECT COUNT(*) FROM documents WHERE processed_at >= :start AND processed_at < :endExclusive) AS documents_processed,
                    (SELECT COUNT(*) FROM documents WHERE status = 'READY' AND processed_at >= :start AND processed_at < :endExclusive) AS documents_ready,
                    (SELECT COUNT(*) FROM documents WHERE status = 'FAILED' AND processed_at >= :start AND processed_at < :endExclusive) AS documents_failed,
                    (SELECT COALESCE(SUM(total_tokens), 0) FROM ai_usage_event) AS total_ai_tokens_used,
                    (SELECT COALESCE(SUM(total_tokens), 0) FROM ai_usage_event WHERE created_at >= :start AND created_at < :endExclusive) AS ai_tokens_used,
                    (SELECT COALESCE(SUM(estimated_cost_micros), 0) FROM ai_usage_event WHERE created_at >= :start AND created_at < :endExclusive) AS estimated_ai_cost_micros
                """;
        return jdbcTemplate.queryForObject(sql, parameters(start, endExclusive), (rs, rowNum) -> {
            long latency = rs.getLong("average_review_latency_millis");
            return new SystemUsageMetricsResponse.Summary(rs.getLong("total_users"), rs.getLong("active_users"),
                    rs.getLong("new_users"), rs.getLong("compositions_created"), rs.getLong("active_learners"),
                    rs.getLong("review_requests"), rs.getLong("successful_reviews"), rs.getLong("failed_reviews"),
                    rs.wasNull() ? null : latency, rs.getLong("documents_uploaded"), rs.getLong("documents_processed"),
                    rs.getLong("documents_ready"), rs.getLong("documents_failed"), rs.getLong("total_ai_tokens_used"),
                    rs.getLong("ai_tokens_used"), rs.getLong("estimated_ai_cost_micros"));
        });
    }

    @Override
    public List<DailyUsageMetric> getDailyActivity(LocalDateTime start, LocalDateTime endExclusive) {
        return jdbcTemplate.query(SYSTEM_DAILY_ACTIVITY_SQL, parameters(start, endExclusive), dailyMapper());
    }

    @Override
    public List<RetentionCohortMetric> getRetentionCohorts(LocalDateTime start, LocalDateTime endExclusive) {
        String sql = """
                WITH cohorts AS (
                    SELECT id AS user_id, date_trunc('week', created_at)::date AS cohort_start
                    FROM app_user WHERE deleted_at IS NULL AND created_at >= :start AND created_at < :endExclusive
                ), activity AS (
                    SELECT user_id, date_trunc('week', created_at)::date AS activity_week FROM composition WHERE created_at >= :start AND created_at < :endExclusive
                    UNION SELECT c.user_id, date_trunc('week', e.evaluated_at)::date FROM evaluation e JOIN composition c ON c.id = e.composition_id WHERE e.evaluated_at >= :start AND e.evaluated_at < :endExclusive
                    UNION SELECT user_id, date_trunc('week', created_at)::date FROM documents WHERE created_at >= :start AND created_at < :endExclusive
                )
                SELECT c.cohort_start, COUNT(DISTINCT c.user_id) AS cohort_size,
                    COUNT(DISTINCT a.user_id) FILTER (WHERE a.activity_week = c.cohort_start) AS week_0,
                    COUNT(DISTINCT a.user_id) FILTER (WHERE a.activity_week = c.cohort_start + 7) AS week_1,
                    COUNT(DISTINCT a.user_id) FILTER (WHERE a.activity_week = c.cohort_start + 14) AS week_2,
                    COUNT(DISTINCT a.user_id) FILTER (WHERE a.activity_week = c.cohort_start + 21) AS week_3,
                    COUNT(DISTINCT a.user_id) FILTER (WHERE a.activity_week = c.cohort_start + 28) AS week_4
                FROM cohorts c LEFT JOIN activity a ON a.user_id = c.user_id
                GROUP BY c.cohort_start ORDER BY c.cohort_start
                """;
        return jdbcTemplate.query(sql, parameters(start, endExclusive), (rs, rowNum) -> new RetentionCohortMetric(
                rs.getDate("cohort_start").toLocalDate(), rs.getLong("cohort_size"), rs.getLong("week_0"),
                rs.getLong("week_1"), rs.getLong("week_2"), rs.getLong("week_3"), rs.getLong("week_4")));
    }

    @Override
    public UserUsageMetricsResponse.Summary getUserSummary(long userId, LocalDateTime start, LocalDateTime endExclusive) {
        String sql = """
                WITH activity AS (
                    SELECT created_at AS activity_at FROM composition WHERE user_id = :userId AND created_at >= :start AND created_at < :endExclusive
                    UNION ALL SELECT e.evaluated_at FROM evaluation e JOIN composition c ON c.id = e.composition_id WHERE c.user_id = :userId AND e.evaluated_at >= :start AND e.evaluated_at < :endExclusive
                    UNION ALL SELECT created_at FROM documents WHERE user_id = :userId AND created_at >= :start AND created_at < :endExclusive
                )
                SELECT
                    (SELECT COUNT(DISTINCT activity_at::date) FROM activity) AS active_days,
                    (SELECT COUNT(*) FROM composition WHERE user_id = :userId AND created_at >= :start AND created_at < :endExclusive) AS compositions_created,
                    (SELECT COUNT(*) FROM evaluation e JOIN composition c ON c.id = e.composition_id WHERE c.user_id = :userId AND e.requested_at >= :start AND e.requested_at < :endExclusive) AS review_requests,
                    (SELECT COUNT(*) FROM evaluation e JOIN composition c ON c.id = e.composition_id WHERE c.user_id = :userId AND e.success = TRUE AND e.evaluated_at >= :start AND e.evaluated_at < :endExclusive) AS successful_reviews,
                    (SELECT COUNT(*) FROM evaluation e JOIN composition c ON c.id = e.composition_id WHERE c.user_id = :userId AND e.success = FALSE AND e.evaluated_at >= :start AND e.evaluated_at < :endExclusive) AS failed_reviews,
                    (SELECT COUNT(*) FROM documents WHERE user_id = :userId AND created_at >= :start AND created_at < :endExclusive) AS documents_uploaded,
                    (SELECT COUNT(*) FROM documents WHERE user_id = :userId AND status = 'READY' AND processed_at >= :start AND processed_at < :endExclusive) AS documents_ready,
                    (SELECT COUNT(*) FROM documents WHERE user_id = :userId AND status = 'FAILED' AND processed_at >= :start AND processed_at < :endExclusive) AS documents_failed,
                    (SELECT COALESCE(SUM(total_tokens), 0) FROM ai_usage_event WHERE user_id = :userId AND created_at >= :start AND created_at < :endExclusive) AS ai_tokens_used,
                    (SELECT COALESCE(SUM(estimated_cost_micros), 0) FROM ai_usage_event WHERE user_id = :userId AND created_at >= :start AND created_at < :endExclusive) AS estimated_ai_cost_micros,
                    (SELECT MAX(activity_at) FROM activity) AS last_activity_at
                """;
        return jdbcTemplate.queryForObject(sql, userParameters(userId, start, endExclusive), (rs, rowNum) -> {
            Timestamp lastActivity = rs.getTimestamp("last_activity_at");
            Instant instant = lastActivity == null ? null : lastActivity.toInstant();
            return new UserUsageMetricsResponse.Summary(rs.getLong("active_days"), rs.getLong("compositions_created"),
                    rs.getLong("review_requests"), rs.getLong("successful_reviews"), rs.getLong("failed_reviews"),
                    rs.getLong("documents_uploaded"), rs.getLong("documents_ready"), rs.getLong("documents_failed"),
                    rs.getLong("ai_tokens_used"), rs.getLong("estimated_ai_cost_micros"), instant);
        });
    }

    @Override
    public List<DailyUsageMetric> getUserDailyActivity(long userId, LocalDateTime start, LocalDateTime endExclusive) {
        return jdbcTemplate.query(USER_DAILY_ACTIVITY_SQL, userParameters(userId, start, endExclusive), dailyMapper());
    }

    private static RowMapper<DailyUsageMetric> dailyMapper() {
        return (rs, rowNum) -> new DailyUsageMetric(rs.getDate("metric_date").toLocalDate(), rs.getLong("new_users"),
                rs.getLong("compositions_created"), rs.getLong("successful_reviews"), rs.getLong("documents_uploaded"),
                rs.getLong("ai_tokens_used"), rs.getLong("estimated_ai_cost_micros"));
    }

    private static MapSqlParameterSource parameters(LocalDateTime start, LocalDateTime endExclusive) {
        return new MapSqlParameterSource().addValue("start", start).addValue("endExclusive", endExclusive);
    }

    private static MapSqlParameterSource userParameters(long userId, LocalDateTime start, LocalDateTime endExclusive) {
        return parameters(start, endExclusive).addValue("userId", userId);
    }
}
