package com.nus.iss.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.nus.iss.dto.DailyUsageMetric;
import com.nus.iss.dto.SystemUsageMetricsResponse;

import lombok.RequiredArgsConstructor;

/**
 * Read-only cross-table reporting queries. The application currently uses one
 * PostgreSQL database, so this repository deliberately performs aggregation in
 * the database rather than loading individual records into a service.
 */
@Repository
@RequiredArgsConstructor
public class UsageMetricsRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public SystemUsageMetricsResponse.Summary summary(LocalDateTime start, LocalDateTime endExclusive) {
        String sql = """
                SELECT
                    (SELECT COUNT(*) FROM app_user WHERE deleted_at IS NULL) AS total_users,
                    (SELECT COUNT(*) FROM app_user WHERE deleted_at IS NULL AND status = 'ACTIVE') AS active_users,
                    (SELECT COUNT(*) FROM app_user WHERE deleted_at IS NULL
                        AND created_at >= :start AND created_at < :endExclusive) AS new_users,
                    (SELECT COUNT(*) FROM composition
                        WHERE created_at >= :start AND created_at < :endExclusive) AS compositions_created,
                    (SELECT COUNT(*) FROM composition WHERE composition_status = 'REVIEWED'
                        AND updated_at >= :start AND updated_at < :endExclusive) AS compositions_reviewed,
                    (SELECT COUNT(*) FROM documents
                        WHERE created_at >= :start AND created_at < :endExclusive) AS documents_uploaded,
                    (SELECT COALESCE(SUM(ai_token_used), 0) FROM app_user WHERE deleted_at IS NULL) AS ai_tokens_used
                """;

        return jdbcTemplate.queryForObject(sql, parameters(start, endExclusive), (rs, rowNum) ->
                new SystemUsageMetricsResponse.Summary(
                        rs.getLong("total_users"),
                        rs.getLong("active_users"),
                        rs.getLong("new_users"),
                        rs.getLong("compositions_created"),
                        rs.getLong("compositions_reviewed"),
                        rs.getLong("documents_uploaded"),
                        rs.getLong("ai_tokens_used")));
    }

    public List<DailyUsageMetric> daily(LocalDateTime start, LocalDateTime endExclusive) {
        String sql = """
                WITH dates AS (
                    SELECT generate_series(CAST(:start AS date), CAST(:endExclusive AS date) - INTERVAL '1 day', INTERVAL '1 day')::date AS metric_date
                ), users_by_day AS (
                    SELECT created_at::date AS metric_date, COUNT(*) AS value
                    FROM app_user
                    WHERE deleted_at IS NULL AND created_at >= :start AND created_at < :endExclusive
                    GROUP BY created_at::date
                ), compositions_by_day AS (
                    SELECT created_at::date AS metric_date, COUNT(*) AS value
                    FROM composition
                    WHERE created_at >= :start AND created_at < :endExclusive
                    GROUP BY created_at::date
                ), reviewed_by_day AS (
                    SELECT updated_at::date AS metric_date, COUNT(*) AS value
                    FROM composition
                    WHERE composition_status = 'REVIEWED' AND updated_at >= :start AND updated_at < :endExclusive
                    GROUP BY updated_at::date
                ), documents_by_day AS (
                    SELECT created_at::date AS metric_date, COUNT(*) AS value
                    FROM documents
                    WHERE created_at >= :start AND created_at < :endExclusive
                    GROUP BY created_at::date
                )
                SELECT dates.metric_date,
                       COALESCE(users_by_day.value, 0) AS new_users,
                       COALESCE(compositions_by_day.value, 0) AS compositions_created,
                       COALESCE(reviewed_by_day.value, 0) AS compositions_reviewed,
                       COALESCE(documents_by_day.value, 0) AS documents_uploaded
                FROM dates
                LEFT JOIN users_by_day USING (metric_date)
                LEFT JOIN compositions_by_day USING (metric_date)
                LEFT JOIN reviewed_by_day USING (metric_date)
                LEFT JOIN documents_by_day USING (metric_date)
                ORDER BY dates.metric_date
                """;

        return jdbcTemplate.query(sql, parameters(start, endExclusive), (rs, rowNum) -> new DailyUsageMetric(
                rs.getDate("metric_date").toLocalDate(),
                rs.getLong("new_users"),
                rs.getLong("compositions_created"),
                rs.getLong("compositions_reviewed"),
                rs.getLong("documents_uploaded")));
    }

    private static MapSqlParameterSource parameters(LocalDateTime start, LocalDateTime endExclusive) {
        return new MapSqlParameterSource()
                .addValue("start", start)
                .addValue("endExclusive", endExclusive);
    }
}
