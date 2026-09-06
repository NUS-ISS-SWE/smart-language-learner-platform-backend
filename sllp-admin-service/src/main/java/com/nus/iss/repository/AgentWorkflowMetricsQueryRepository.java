package com.nus.iss.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.nus.iss.dto.AgentMetrics;
import com.nus.iss.dto.WorkflowReliabilityMetric;

/** Read-model gateway for assessment workflow and agent reliability reporting. */
public interface AgentWorkflowMetricsQueryRepository {
    WorkflowReliabilityMetric getWorkflowReliability(LocalDateTime start, LocalDateTime endExclusive);

    List<AgentMetrics> getAgentMetrics(LocalDateTime start, LocalDateTime endExclusive);
}
