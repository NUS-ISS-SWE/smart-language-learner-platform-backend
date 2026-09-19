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
import com.nus.iss.dto.AgentMetrics;
import com.nus.iss.dto.WorkflowReliabilityMetric;
import com.nus.iss.dto.UserUsageMetricsResponse;

@Testcontainers(disabledWithoutDocker = true)
class JdbcSystemUsageMetricsQueryRepositoryIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private JdbcTemplate jdbc;
    private JdbcSystemUsageMetricsQueryRepository systemRepository;
    private JdbcAgentWorkflowMetricsQueryRepository agentWorkflowRepository;
    private JdbcUserUsageMetricsQueryRepository userRepository;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        jdbc = new JdbcTemplate(dataSource);
        NamedParameterJdbcTemplate namedJdbc = new NamedParameterJdbcTemplate(dataSource);
        systemRepository = new JdbcSystemUsageMetricsQueryRepository(namedJdbc);
        agentWorkflowRepository = new JdbcAgentWorkflowMetricsQueryRepository(namedJdbc);
        userRepository = new JdbcUserUsageMetricsQueryRepository(namedJdbc);
        jdbc.execute("DROP TABLE IF EXISTS agent_workflow_step_log, agent_workflow_run, ai_usage_event, documents, evaluation, composition, app_user");
        jdbc.execute("CREATE TABLE app_user (id BIGINT PRIMARY KEY, created_at TIMESTAMP NOT NULL, deleted_at TIMESTAMP, status VARCHAR(50) NOT NULL)");
        jdbc.execute("CREATE TABLE composition (id BIGINT PRIMARY KEY, user_id BIGINT NOT NULL, created_at TIMESTAMP NOT NULL)");
        jdbc.execute("CREATE TABLE evaluation (composition_id BIGINT NOT NULL, request_id UUID, success BOOLEAN NOT NULL, requested_at TIMESTAMP, evaluated_at TIMESTAMP NOT NULL)");
        jdbc.execute("CREATE TABLE documents (user_id BIGINT NOT NULL, created_at TIMESTAMP NOT NULL, processed_at TIMESTAMP, status VARCHAR(50) NOT NULL)");
        jdbc.execute("CREATE TABLE ai_usage_event (event_id UUID PRIMARY KEY, request_id UUID, user_id BIGINT NOT NULL, agent VARCHAR(50) NOT NULL, created_at TIMESTAMP NOT NULL, total_tokens BIGINT NOT NULL, estimated_cost_micros BIGINT NOT NULL)");
        jdbc.execute("CREATE TABLE agent_workflow_run (workflow_id UUID PRIMARY KEY, source_event_id VARCHAR(255) NOT NULL UNIQUE, user_id BIGINT NOT NULL, status VARCHAR(32) NOT NULL, created_at TIMESTAMPTZ NOT NULL, completed_at TIMESTAMPTZ)");
        jdbc.execute("CREATE TABLE agent_workflow_step_log (id BIGSERIAL PRIMARY KEY, workflow_id UUID NOT NULL, step VARCHAR(64) NOT NULL, status VARCHAR(32) NOT NULL, attempt INTEGER NOT NULL, latency_ms INTEGER, details JSONB, created_at TIMESTAMPTZ NOT NULL)");
    }

    @Test
    void returnsAccurateUsageReliabilityAndDailyMetrics() {
        insert("INSERT INTO app_user VALUES (?, ?, ?, ?)", 1L, at("2026-08-01T09:00"), null, "ACTIVE");
        insert("INSERT INTO app_user VALUES (?, ?, ?, ?)", 2L, at("2026-08-02T10:00"), null, "INACTIVE");
        insert("INSERT INTO app_user VALUES (?, ?, ?, ?)", 3L, at("2026-08-02T10:00"), at("2026-08-02T11:00"), "ACTIVE");
        insert("INSERT INTO composition VALUES (?, ?, ?)", 1L, 1L, at("2026-08-01T12:00"));
        insert("INSERT INTO composition VALUES (?, ?, ?)", 2L, 2L, at("2026-08-03T12:00"));
        insert("INSERT INTO evaluation VALUES (?, ?, ?, ?, ?)", 1L, UUID.randomUUID(), true, at("2026-08-02T12:00"), at("2026-08-02T13:00"));
        insert("INSERT INTO evaluation VALUES (?, ?, ?, ?, ?)", 2L, UUID.randomUUID(), false, at("2026-08-03T12:00"), at("2026-08-03T13:00"));
        insert("INSERT INTO documents VALUES (?, ?, ?, ?)", 2L, at("2026-08-02T14:00"), at("2026-08-02T14:01"), "READY");
        insert("INSERT INTO ai_usage_event VALUES (?, ?, ?, ?, ?, ?, ?)", UUID.randomUUID(), UUID.randomUUID(), 1L, "grading", at("2026-08-02T13:00"), 100L, 15L);

        UUID completedWorkflow = UUID.randomUUID();
        UUID failedWorkflow = UUID.randomUUID();
        UUID pendingWorkflow = UUID.randomUUID();
        insert("INSERT INTO agent_workflow_run VALUES (?, ?, ?, ?, ?, ?)", completedWorkflow, "workflow-completed", 1L, "COMPLETED", at("2026-08-01T10:00"), at("2026-08-01T10:01"));
        insert("INSERT INTO agent_workflow_run VALUES (?, ?, ?, ?, ?, ?)", failedWorkflow, "workflow-failed", 2L, "FAILED", at("2026-08-02T10:00"), at("2026-08-02T10:03"));
        insert("INSERT INTO agent_workflow_run VALUES (?, ?, ?, ?, ?, ?)", pendingWorkflow, "workflow-pending", 2L, "GRADING", at("2026-08-03T10:00"), null);
        insert("INSERT INTO agent_workflow_step_log (workflow_id, step, status, attempt, details, created_at) VALUES (?, ?, ?, ?, ?::jsonb, ?)", completedWorkflow, "GRADING_ATTEMPT", "GRADING", 1, "{\"agent\":\"grading\",\"status\":\"SUCCEEDED\"}", at("2026-08-01T10:00"));
        insert("INSERT INTO agent_workflow_step_log (workflow_id, step, status, attempt, details, created_at) VALUES (?, ?, ?, ?, ?::jsonb, ?)", failedWorkflow, "GRADING_ATTEMPT", "GRADING", 1, "{\"agent\":\"grading\",\"status\":\"FAILED\"}", at("2026-08-02T10:00"));
        insert("INSERT INTO agent_workflow_step_log (workflow_id, step, status, attempt, details, created_at) VALUES (?, ?, ?, ?, ?::jsonb, ?)", completedWorkflow, "REVIEW_ATTEMPT", "REVIEWING", 1, "{\"agent\":\"review\",\"status\":\"SUCCEEDED\"}", at("2026-08-01T10:00"));

        LocalDateTime start = at("2026-08-01T00:00");
        LocalDateTime end = at("2026-08-04T00:00");

        assertEquals(new SystemUsageMetricsResponse.Summary(2, 1, 2, 2, 2, 5, 1, 1, 50.0, 50.0, 3_600_000L, 1, 1, 1, 0, 100.0, 0.0, 100, 100, 15), systemRepository.getSummary(start, end));
        assertEquals(List.of(
                new DailyUsageMetric(LocalDate.of(2026, 8, 1), 1, 1, 0, 0, 0, 0),
                new DailyUsageMetric(LocalDate.of(2026, 8, 2), 1, 0, 1, 1, 100, 15),
                new DailyUsageMetric(LocalDate.of(2026, 8, 3), 0, 1, 0, 0, 0, 0)), systemRepository.getDailyActivity(start, end));
        assertEquals(List.of(new RetentionCohortMetric(LocalDate.of(2026, 7, 27), 2, 2L, null, null, null, null)),
                systemRepository.getRetentionCohorts(start, end));
        assertEquals(new WorkflowReliabilityMetric(3, 1, 1, 1, 50.0, 50.0, 120_000L),
                agentWorkflowRepository.getWorkflowReliability(start, end));
        assertEquals(List.of(
                new AgentMetrics("grading", 2, 1, 50.0, 100, 15),
                new AgentMetrics("review", 1, 0, 0.0, 0, 0)), agentWorkflowRepository.getAgentMetrics(start, end));
        UserUsageMetricsResponse.Summary userSummary = userRepository.getUserSummary(1L, start, end);
        assertEquals(2, userSummary.activeDays());
        assertEquals(1, userSummary.compositionsCreated());
        assertEquals(2, userSummary.reviewRequests());
        assertEquals(1, userSummary.successfulReviews());
        assertEquals(0, userSummary.failedReviews());
        assertEquals(100, userSummary.aiTokensUsed());
        assertEquals(15, userSummary.estimatedAiCostMicros());
        assertEquals(List.of(
                new DailyUsageMetric(LocalDate.of(2026, 8, 1), 1, 1, 0, 0, 0, 0),
                new DailyUsageMetric(LocalDate.of(2026, 8, 2), 0, 0, 1, 0, 100, 15),
                new DailyUsageMetric(LocalDate.of(2026, 8, 3), 0, 0, 0, 0, 0, 0)),
                userRepository.getUserDailyActivity(1L, start, end));
    }

    @Test
    void usageWithoutRecordedAttemptsHasNoFailureRate() {
        insert("INSERT INTO ai_usage_event VALUES (?, ?, ?, ?, ?, ?, ?)", UUID.randomUUID(), UUID.randomUUID(),
                1L, "grading", at("2026-08-02T13:00"), 100L, 15L);
        assertEquals(List.of(new AgentMetrics("grading", 0, 0, null, 100, 15)),
                agentWorkflowRepository.getAgentMetrics(at("2026-08-01T00:00"), at("2026-08-04T00:00")));
    }

    @Test
    void keepsWorkflowOutcomesInTheReceivedCohort() {
        UUID completedBeforePeriod = UUID.randomUUID();
        UUID completedAfterPeriod = UUID.randomUUID();
        insert("INSERT INTO agent_workflow_run VALUES (?, ?, ?, ?, ?, ?)", completedBeforePeriod, "old-workflow", 1L,
                "COMPLETED", at("2026-07-31T23:00"), at("2026-08-01T01:00"));
        insert("INSERT INTO agent_workflow_run VALUES (?, ?, ?, ?, ?, ?)", completedAfterPeriod, "late-workflow", 1L,
                "COMPLETED", at("2026-08-01T01:00"), at("2026-08-04T01:00"));

        assertEquals(new WorkflowReliabilityMetric(1, 0, 0, 1, 0.0, 0.0, null),
                agentWorkflowRepository.getWorkflowReliability(at("2026-08-01T00:00"), at("2026-08-04T00:00")));
    }

    private void insert(String sql, Object... arguments) { jdbc.update(sql, arguments); }
    private static LocalDateTime at(String value) { return LocalDateTime.parse(value); }
}
