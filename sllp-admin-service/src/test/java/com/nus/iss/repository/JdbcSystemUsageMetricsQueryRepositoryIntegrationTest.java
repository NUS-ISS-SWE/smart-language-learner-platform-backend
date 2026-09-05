package com.nus.iss.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.nus.iss.dto.DailyUsageMetric;
import com.nus.iss.dto.RetentionCohortMetric;
import com.nus.iss.dto.SystemUsageMetricsResponse;

@Testcontainers(disabledWithoutDocker = true)
class JdbcSystemUsageMetricsQueryRepositoryIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private JdbcTemplate jdbc;
    private JdbcSystemUsageMetricsQueryRepository repository;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        jdbc = new JdbcTemplate(dataSource);
        repository = new JdbcSystemUsageMetricsQueryRepository(new NamedParameterJdbcTemplate(dataSource));
        jdbc.execute("DROP TABLE IF EXISTS ai_usage_event, documents, evaluation, composition, app_user");
        jdbc.execute("CREATE TABLE app_user (id BIGINT PRIMARY KEY, created_at TIMESTAMP NOT NULL, deleted_at TIMESTAMP, status VARCHAR(50) NOT NULL)");
        jdbc.execute("CREATE TABLE composition (id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL, created_at TIMESTAMP NOT NULL)");
        jdbc.execute("CREATE TABLE evaluation (composition_id BIGINT NOT NULL, success BOOLEAN NOT NULL, requested_at TIMESTAMP, evaluated_at TIMESTAMP NOT NULL)");
        jdbc.execute("CREATE TABLE documents (user_id BIGINT NOT NULL, created_at TIMESTAMP NOT NULL, processed_at TIMESTAMP, status VARCHAR(50) NOT NULL)");
        jdbc.execute("CREATE TABLE ai_usage_event (request_id UUID PRIMARY KEY, user_id BIGINT NOT NULL, created_at TIMESTAMP NOT NULL, total_tokens BIGINT NOT NULL, estimated_cost_micros BIGINT NOT NULL)");
    }

    @Test
    void returnsAccurateUsageReliabilityAndDailyMetrics() {
        insert("INSERT INTO app_user VALUES (?, ?, ?, ?)", 1L, at("2026-08-01T09:00"), null, "ACTIVE");
        insert("INSERT INTO app_user VALUES (?, ?, ?, ?)", 2L, at("2026-08-02T10:00"), null, "INACTIVE");
        insert("INSERT INTO app_user VALUES (?, ?, ?, ?)", 3L, at("2026-08-02T10:00"), at("2026-08-02T11:00"), "ACTIVE");
        insert("INSERT INTO composition VALUES (?, ?, ?)", 1L, 1L, at("2026-08-01T12:00"));
        insert("INSERT INTO composition VALUES (?, ?, ?)", 2L, 2L, at("2026-08-03T12:00"));
        insert("INSERT INTO evaluation VALUES (?, ?, ?, ?)", 1L, true, at("2026-08-02T12:00"), at("2026-08-02T13:00"));
        insert("INSERT INTO evaluation VALUES (?, ?, ?, ?)", 2L, false, at("2026-08-03T12:00"), at("2026-08-03T13:00"));
        insert("INSERT INTO documents VALUES (?, ?, ?, ?)", 2L, at("2026-08-02T14:00"), at("2026-08-02T14:01"), "READY");
        insert("INSERT INTO ai_usage_event VALUES (?, ?, ?, ?, ?)", UUID.randomUUID(), 1L, at("2026-08-02T13:00"), 100L, 15L);

        LocalDateTime start = at("2026-08-01T00:00");
        LocalDateTime end = at("2026-08-04T00:00");

        assertEquals(new SystemUsageMetricsResponse.Summary(2, 1, 2, 2, 2, 2, 1, 1, 3_600_000L, 1, 1, 1, 0, 100, 100, 15), repository.getSummary(start, end));
        assertEquals(List.of(
                new DailyUsageMetric(LocalDate.of(2026, 8, 1), 1, 1, 0, 0, 0, 0),
                new DailyUsageMetric(LocalDate.of(2026, 8, 2), 1, 0, 1, 1, 100, 15),
                new DailyUsageMetric(LocalDate.of(2026, 8, 3), 0, 1, 0, 0, 0, 0)), repository.getDailyActivity(start, end));
        assertEquals(List.of(new RetentionCohortMetric(LocalDate.of(2026, 7, 27), 2, 2, 0, 0, 0, 0)),
                repository.getRetentionCohorts(start, end));
    }

    private void insert(String sql, Object... arguments) { jdbc.update(sql, arguments); }
    private static LocalDateTime at(String value) { return LocalDateTime.parse(value); }
}
