package com.nus.iss.repository;

import com.nus.iss.dto.AgentMetrics;
import com.nus.iss.dto.WorkflowReliabilityMetric;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JdbcAgentWorkflowMetricsQueryRepositoryTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;
    @Mock
    private ResultSet resultSet;

    private JdbcAgentWorkflowMetricsQueryRepository repository;
    private final LocalDateTime start = LocalDate.of(2026, 9, 1).atStartOfDay();
    private final LocalDateTime end = LocalDate.of(2026, 10, 1).atStartOfDay();

    @BeforeEach
    void setUp() {
        repository = new JdbcAgentWorkflowMetricsQueryRepository(jdbcTemplate);
    }

    @SuppressWarnings("unchecked")
    @Test
    void getWorkflowReliabilityMapsValues() throws Exception {
        when(resultSet.getLong(anyString())).thenReturn(4L);
        when(resultSet.getDouble("completion_percent")).thenReturn(75.0);
        when(resultSet.getDouble("failure_percent")).thenReturn(25.0);
        when(resultSet.wasNull()).thenReturn(false);
        when(jdbcTemplate.queryForObject(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> ((RowMapper<?>) invocation.getArgument(2)).mapRow(resultSet, 0));

        WorkflowReliabilityMetric result = repository.getWorkflowReliability(start, end);

        assertEquals(4L, result.received());
        assertEquals(75.0, result.completionPercent());
        assertEquals(25.0, result.failurePercent());
        assertEquals(4L, result.averageCompletionLatencyMillis());
    }

    @SuppressWarnings("unchecked")
    @Test
    void getWorkflowReliabilityMapsSqlNulls() throws Exception {
        when(resultSet.getLong(anyString())).thenReturn(0L);
        when(resultSet.getDouble(anyString())).thenReturn(0.0);
        when(resultSet.wasNull()).thenReturn(true);
        when(jdbcTemplate.queryForObject(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> ((RowMapper<?>) invocation.getArgument(2)).mapRow(resultSet, 0));

        WorkflowReliabilityMetric result = repository.getWorkflowReliability(start, end);

        assertNull(result.completionPercent());
        assertNull(result.failurePercent());
        assertNull(result.averageCompletionLatencyMillis());
    }

    @SuppressWarnings("unchecked")
    @Test
    void getAgentMetricsMapsRowsIncludingNullablePercentage() throws Exception {
        when(resultSet.getString("agent")).thenReturn("grading", "review");
        when(resultSet.getLong(anyString())).thenReturn(3L);
        when(resultSet.getDouble("failure_percent")).thenReturn(33.33, 0.0);
        when(resultSet.wasNull()).thenReturn(false, true);
        when(jdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(2);
                    return List.of(mapper.mapRow(resultSet, 0), mapper.mapRow(resultSet, 1));
                });

        List<AgentMetrics> result = repository.getAgentMetrics(start, end);

        assertEquals("grading", result.get(0).agent());
        assertEquals(33.33, result.get(0).failurePercent());
        assertEquals("review", result.get(1).agent());
        assertNull(result.get(1).failurePercent());
    }
}
