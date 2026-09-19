package com.nus.iss.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nus.iss.dto.DailyUsageMetric;
import com.nus.iss.dto.SystemUsageMetricsResponse;
import com.nus.iss.dto.RetentionCohortMetric;
import com.nus.iss.dto.UserUsageMetricsResponse;
import com.nus.iss.dto.WorkflowReliabilityMetric;
import com.nus.iss.dto.AgentMetrics;
import com.nus.iss.repository.SystemUsageMetricsQueryRepository;
import com.nus.iss.repository.AgentWorkflowMetricsQueryRepository;
import com.nus.iss.repository.UserUsageMetricsQueryRepository;

@ExtendWith(MockitoExtension.class)
class SystemUsageMetricsServiceImplTest {

    @Mock
    private SystemUsageMetricsQueryRepository systemRepository;
    @Mock
    private AgentWorkflowMetricsQueryRepository agentWorkflowRepository;
    @Mock
    private UserUsageMetricsQueryRepository userRepository;

    @Test
    void returnsAnInclusiveDateRange() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 3);
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime endExclusive = to.plusDays(1).atStartOfDay();
        SystemUsageMetricsResponse.Summary summary = new SystemUsageMetricsResponse.Summary(10, 8, 2, 4, 5, 6, 3, 1, 75.0, 25.0, 500L, 2, 2, 1, 1, 50.0, 50.0, 99, 30, 45);
        List<DailyUsageMetric> daily = List.of(new DailyUsageMetric(from, 1, 2, 1, 0, 30, 45));
        List<RetentionCohortMetric> cohorts = List.of(new RetentionCohortMetric(from, 2, 1L, 1L, 0L, 0L, 0L));
        WorkflowReliabilityMetric workflowReliability = new WorkflowReliabilityMetric(3, 2, 1, 0, 66.67, 33.33, 500L);
        List<AgentMetrics> agentMetrics = List.of(new AgentMetrics("grading", 3, 1, 33.33, 90, 45));
        when(systemRepository.getSummary(start, endExclusive)).thenReturn(summary);
        when(systemRepository.getDailyActivity(start, endExclusive)).thenReturn(daily);
        when(systemRepository.getRetentionCohorts(start, endExclusive)).thenReturn(cohorts);
        when(agentWorkflowRepository.getWorkflowReliability(start, endExclusive)).thenReturn(workflowReliability);
        when(agentWorkflowRepository.getAgentMetrics(start, endExclusive)).thenReturn(agentMetrics);

        SystemUsageMetricsResponse result = new SystemUsageMetricsServiceImpl(systemRepository, agentWorkflowRepository, userRepository).getMetrics(from, to);

        assertEquals(from, result.from());
        assertEquals(to, result.to());
        assertEquals(summary, result.summary());
        assertEquals(daily, result.daily());
        assertEquals(cohorts, result.retentionCohorts());
        assertEquals(workflowReliability, result.workflowReliability());
        assertEquals(agentMetrics, result.agentMetrics());
        verify(systemRepository).getSummary(start, endExclusive);
        verify(systemRepository).getDailyActivity(start, endExclusive);
        verify(systemRepository).getRetentionCohorts(start, endExclusive);
        verify(agentWorkflowRepository).getWorkflowReliability(start, endExclusive);
        verify(agentWorkflowRepository).getAgentMetrics(start, endExclusive);
    }

    @Test
    void rejectsInvalidDateRanges() {
        SystemUsageMetricsServiceImpl service = new SystemUsageMetricsServiceImpl(systemRepository, agentWorkflowRepository, userRepository);

        assertThrows(IllegalArgumentException.class,
                () -> service.getMetrics(LocalDate.of(2026, 8, 2), LocalDate.of(2026, 8, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> service.getMetrics(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 2)));
    }

    @Test
    void returnsMetricsForOneUser() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 3);
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime endExclusive = to.plusDays(1).atStartOfDay();
        UserUsageMetricsResponse.Summary summary = new UserUsageMetricsResponse.Summary(
                2, 1, 1, 1, 0, 100.0, 0.0, 0, 0, 0, 0, null, null, 99, 45, null);
        List<DailyUsageMetric> daily = List.of(new DailyUsageMetric(from, 1, 1, 0, 0, 0, 0));
        when(userRepository.getUserSummary(42L, start, endExclusive)).thenReturn(summary);
        when(userRepository.getUserDailyActivity(42L, start, endExclusive)).thenReturn(daily);

        UserUsageMetricsResponse result = new SystemUsageMetricsServiceImpl(systemRepository, agentWorkflowRepository, userRepository).getUserMetrics(42L, from, to);

        assertEquals(42L, result.userId());
        assertEquals(summary, result.summary());
        assertEquals(daily, result.daily());
        verify(userRepository).getUserSummary(42L, start, endExclusive);
        verify(userRepository).getUserDailyActivity(42L, start, endExclusive);
    }
}
