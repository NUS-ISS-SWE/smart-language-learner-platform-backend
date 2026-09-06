package com.nus.iss.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.nus.iss.dto.AgentMetrics;
import com.nus.iss.dto.WorkflowReliabilityMetric;

import lombok.RequiredArgsConstructor;

/** PostgreSQL read model for assessment workflow outcomes and individual agents. */
@Repository
@RequiredArgsConstructor
public class JdbcAgentWorkflowMetricsQueryRepository implements AgentWorkflowMetricsQueryRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public WorkflowReliabilityMetric getWorkflowReliability(LocalDateTime start, LocalDateTime endExclusive) {
        String sql = """
                WITH received AS (
                    SELECT workflow_id, status FROM agent_workflow_run
                    WHERE created_at >= (:start AT TIME ZONE 'UTC') AND created_at < (:endExclusive AT TIME ZONE 'UTC')
                ), terminal_outcomes AS (
                    SELECT status, created_at, completed_at FROM agent_workflow_run
                    WHERE status IN ('COMPLETED', 'FAILED')
                      AND completed_at >= (:start AT TIME ZONE 'UTC') AND completed_at < (:endExclusive AT TIME ZONE 'UTC')
                )
                SELECT
                    (SELECT COUNT(*) FROM received) AS received,
                    (SELECT COUNT(*) FROM terminal_outcomes WHERE status = 'COMPLETED') AS completed,
                    (SELECT COUNT(*) FROM terminal_outcomes WHERE status = 'FAILED') AS failed,
                    (SELECT COUNT(*) FROM received WHERE status NOT IN ('COMPLETED', 'FAILED')) AS pending,
                    (SELECT ROUND(100.0 * COUNT(*) FILTER (WHERE status = 'COMPLETED') / NULLIF(COUNT(*), 0), 2) FROM terminal_outcomes) AS completion_percent,
                    (SELECT ROUND(100.0 * COUNT(*) FILTER (WHERE status = 'FAILED') / NULLIF(COUNT(*), 0), 2) FROM terminal_outcomes) AS failure_percent,
                    (SELECT ROUND(AVG(EXTRACT(EPOCH FROM (completed_at - created_at)) * 1000)) FROM terminal_outcomes) AS average_completion_latency_millis
                """;
        return jdbcTemplate.queryForObject(sql, parameters(start, endExclusive), (rs, rowNum) -> {
            long latency = rs.getLong("average_completion_latency_millis");
            boolean latencyNull = rs.wasNull();
            double completionPercent = rs.getDouble("completion_percent");
            boolean completionPercentNull = rs.wasNull();
            double failurePercent = rs.getDouble("failure_percent");
            boolean failurePercentNull = rs.wasNull();
            return new WorkflowReliabilityMetric(rs.getLong("received"), rs.getLong("completed"), rs.getLong("failed"),
                    rs.getLong("pending"), completionPercentNull ? null : completionPercent,
                    failurePercentNull ? null : failurePercent, latencyNull ? null : latency);
        });
    }

    @Override
    public List<AgentMetrics> getAgentMetrics(LocalDateTime start, LocalDateTime endExclusive) {
        String sql = """
                WITH attempts AS (
                    SELECT details->>'agent' AS agent, details->>'status' AS attempt_status
                    FROM agent_workflow_step_log
                    WHERE step IN ('GRADING_ATTEMPT', 'REVIEW_ATTEMPT', 'SUGGESTION_ATTEMPT')
                      AND created_at >= (:start AT TIME ZONE 'UTC') AND created_at < (:endExclusive AT TIME ZONE 'UTC')
                ), usage AS (
                    SELECT agent, COALESCE(SUM(total_tokens), 0) AS tokens, COALESCE(SUM(estimated_cost_micros), 0) AS cost
                    FROM ai_usage_event WHERE created_at >= :start AND created_at < :endExclusive GROUP BY agent
                ), agents AS (
                    SELECT agent FROM attempts WHERE agent IS NOT NULL UNION SELECT agent FROM usage
                )
                SELECT agents.agent, COUNT(attempts.agent) AS attempts,
                    COUNT(attempts.agent) FILTER (WHERE attempts.attempt_status <> 'SUCCEEDED') AS failed_attempts,
                    ROUND(100.0 * COUNT(attempts.agent) FILTER (WHERE attempts.attempt_status <> 'SUCCEEDED')
                        / NULLIF(COUNT(attempts.agent), 0), 2) AS failure_percent,
                    COALESCE(MAX(usage.tokens), 0) AS ai_tokens_used, COALESCE(MAX(usage.cost), 0) AS estimated_ai_cost_micros
                FROM agents LEFT JOIN attempts ON attempts.agent = agents.agent LEFT JOIN usage ON usage.agent = agents.agent
                GROUP BY agents.agent ORDER BY agents.agent
                """;
        return jdbcTemplate.query(sql, parameters(start, endExclusive), (rs, rowNum) -> {
            double failurePercent = rs.getDouble("failure_percent");
            boolean failurePercentNull = rs.wasNull();
            return new AgentMetrics(rs.getString("agent"), rs.getLong("attempts"), rs.getLong("failed_attempts"),
                    failurePercentNull ? null : failurePercent, rs.getLong("ai_tokens_used"), rs.getLong("estimated_ai_cost_micros"));
        });
    }

    private static MapSqlParameterSource parameters(LocalDateTime start, LocalDateTime endExclusive) {
        return new MapSqlParameterSource().addValue("start", start).addValue("endExclusive", endExclusive);
    }
}
