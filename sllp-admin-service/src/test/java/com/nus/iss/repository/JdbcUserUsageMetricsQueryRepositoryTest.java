package com.nus.iss.repository;

import com.nus.iss.dto.DailyUsageMetric;
import com.nus.iss.dto.UserUsageMetricsResponse;
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
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JdbcUserUsageMetricsQueryRepositoryTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;
    @Mock
    private ResultSet resultSet;

    private JdbcUserUsageMetricsQueryRepository repository;
    private final LocalDateTime start = LocalDate.of(2026, 9, 1).atStartOfDay();
    private final LocalDateTime end = LocalDate.of(2026, 10, 1).atStartOfDay();

    @BeforeEach
    void setUp() {
        repository = new JdbcUserUsageMetricsQueryRepository(jdbcTemplate);
    }

    @SuppressWarnings("unchecked")
    @Test
    void getUserSummaryMapsValuesAndLastActivity() throws Exception {
        when(resultSet.getTimestamp("last_activity_at"))
                .thenReturn(Timestamp.valueOf("2026-09-20 12:30:00"));
        when(resultSet.getLong(anyString())).thenReturn(5L);
        when(resultSet.getDouble(anyString())).thenReturn(75.0);
        when(resultSet.wasNull()).thenReturn(false);
        when(jdbcTemplate.queryForObject(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> ((RowMapper<?>) invocation.getArgument(2)).mapRow(resultSet, 0));

        UserUsageMetricsResponse.Summary result = repository.getUserSummary(42L, start, end);

        assertEquals(5L, result.activeDays());
        assertEquals(75.0, result.reviewSuccessPercent());
        assertEquals(Instant.parse("2026-09-20T12:30:00Z"), result.lastActivityAt());
    }

    @SuppressWarnings("unchecked")
    @Test
    void getUserSummaryMapsNullableValues() throws Exception {
        when(resultSet.getTimestamp("last_activity_at")).thenReturn(null);
        when(resultSet.getLong(anyString())).thenReturn(0L);
        when(resultSet.getDouble(anyString())).thenReturn(0.0);
        when(resultSet.wasNull()).thenReturn(true);
        when(jdbcTemplate.queryForObject(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> ((RowMapper<?>) invocation.getArgument(2)).mapRow(resultSet, 0));

        UserUsageMetricsResponse.Summary result = repository.getUserSummary(42L, start, end);

        assertNull(result.lastActivityAt());
        assertNull(result.reviewSuccessPercent());
        assertNull(result.reviewFailurePercent());
        assertNull(result.documentSuccessPercent());
        assertNull(result.documentFailurePercent());
    }

    @SuppressWarnings("unchecked")
    @Test
    void getUserDailyActivityMapsRow() throws Exception {
        when(resultSet.getDate("metric_date")).thenReturn(Date.valueOf("2026-09-03"));
        when(resultSet.getLong(anyString())).thenReturn(2L);
        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> List.of(((RowMapper<?>) invocation.getArgument(2)).mapRow(resultSet, 0)));

        List<DailyUsageMetric> result = repository.getUserDailyActivity(42L, start, end);

        assertEquals(LocalDate.of(2026, 9, 3), result.getFirst().date());
        assertEquals(2L, result.getFirst().compositionsCreated());
    }
}
