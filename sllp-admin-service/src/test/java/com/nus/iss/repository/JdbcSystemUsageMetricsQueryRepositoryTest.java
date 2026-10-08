package com.nus.iss.repository;

import com.nus.iss.dto.DailyUsageMetric;
import com.nus.iss.dto.RetentionCohortMetric;
import com.nus.iss.dto.SystemUsageMetricsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Date;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JdbcSystemUsageMetricsQueryRepositoryTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;
    @Mock
    private ResultSet resultSet;

    private JdbcSystemUsageMetricsQueryRepository repository;
    private final LocalDateTime start = LocalDate.of(2026, 9, 1).atStartOfDay();
    private final LocalDateTime end = LocalDate.of(2026, 10, 1).atStartOfDay();

    @BeforeEach
    void setUp() {
        repository = new JdbcSystemUsageMetricsQueryRepository(jdbcTemplate);
    }

    @SuppressWarnings("unchecked")
    @Test
    void getSummaryMapsAllValues() throws Exception {
        Map<String, Long> longs = Map.ofEntries(
                Map.entry("average_review_latency_millis", 250L), Map.entry("total_users", 100L),
                Map.entry("active_users", 80L), Map.entry("new_users", 10L),
                Map.entry("compositions_created", 30L), Map.entry("active_learners", 20L),
                Map.entry("review_requests", 25L), Map.entry("successful_reviews", 20L),
                Map.entry("failed_reviews", 5L), Map.entry("documents_uploaded", 12L),
                Map.entry("documents_processed", 10L), Map.entry("documents_ready", 9L),
                Map.entry("documents_failed", 1L), Map.entry("total_ai_tokens_used", 1000L),
                Map.entry("ai_tokens_used", 300L), Map.entry("estimated_ai_cost_micros", 70L));
        when(resultSet.getLong(anyString())).thenAnswer(invocation -> longs.get(invocation.getArgument(0)));
        when(resultSet.getDouble(anyString())).thenAnswer(invocation ->
                invocation.<String>getArgument(0).contains("failure") ? 10.0 : 90.0);
        when(resultSet.wasNull()).thenReturn(false);
        when(jdbcTemplate.queryForObject(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> ((RowMapper<?>) invocation.getArgument(2)).mapRow(resultSet, 0));

        SystemUsageMetricsResponse.Summary result = repository.getSummary(start, end);

        assertEquals(100L, result.totalUsers());
        assertEquals(250L, result.averageReviewLatencyMillis());
        assertEquals(90.0, result.reviewSuccessPercent());
        assertEquals(10.0, result.documentFailurePercent());
    }

    @SuppressWarnings("unchecked")
    @Test
    void getSummaryMapsSqlNullsAsNull() throws Exception {
        when(resultSet.getLong(anyString())).thenReturn(0L);
        when(resultSet.getDouble(anyString())).thenReturn(0.0);
        when(resultSet.wasNull()).thenReturn(true);
        when(jdbcTemplate.queryForObject(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> ((RowMapper<?>) invocation.getArgument(2)).mapRow(resultSet, 0));

        SystemUsageMetricsResponse.Summary result = repository.getSummary(start, end);

        assertNull(result.averageReviewLatencyMillis());
        assertNull(result.reviewSuccessPercent());
        assertNull(result.reviewFailurePercent());
        assertNull(result.documentSuccessPercent());
        assertNull(result.documentFailurePercent());
    }

    @SuppressWarnings("unchecked")
    @Test
    void getDailyActivityMapsRowsAndParameters() throws Exception {
        when(resultSet.getDate("metric_date")).thenReturn(Date.valueOf("2026-09-02"));
        when(resultSet.getLong(anyString())).thenReturn(4L);
        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> List.of(((RowMapper<?>) invocation.getArgument(2)).mapRow(resultSet, 0)));

        List<DailyUsageMetric> result = repository.getDailyActivity(start, end);

        assertEquals(LocalDate.of(2026, 9, 2), result.getFirst().date());
        assertEquals(4L, result.getFirst().newUsers());
        assertEquals(4L, result.getFirst().estimatedAiCostMicros());
    }

    @SuppressWarnings("unchecked")
    @Test
    void getRetentionCohortsMapsRows() throws Exception {
        when(resultSet.getDate("cohort_start")).thenReturn(Date.valueOf("2026-09-01"));
        when(resultSet.getLong("cohort_size")).thenReturn(8L);
        when(resultSet.getObject(anyString(), org.mockito.ArgumentMatchers.eq(Long.class)))
                .thenAnswer(invocation -> "week_4".equals(invocation.getArgument(0)) ? null : 3L);
        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> List.of(((RowMapper<?>) invocation.getArgument(2)).mapRow(resultSet, 0)));

        List<RetentionCohortMetric> result = repository.getRetentionCohorts(start, end);

        assertEquals(8L, result.getFirst().cohortSize());
        assertEquals(3L, result.getFirst().week0Retained());
        assertNull(result.getFirst().week4Retained());
    }
}
