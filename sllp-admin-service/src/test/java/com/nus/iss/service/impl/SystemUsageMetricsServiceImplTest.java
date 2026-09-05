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
import com.nus.iss.repository.SystemUsageMetricsQueryRepository;

@ExtendWith(MockitoExtension.class)
class SystemUsageMetricsServiceImplTest {

    @Mock
    private SystemUsageMetricsQueryRepository repository;

    @Test
    void returnsAnInclusiveDateRange() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 3);
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime endExclusive = to.plusDays(1).atStartOfDay();
        SystemUsageMetricsResponse.Summary summary = new SystemUsageMetricsResponse.Summary(10, 8, 2, 4, 5, 6, 3, 1, 500L, 2, 2, 1, 1, 99, 30, 45);
        List<DailyUsageMetric> daily = List.of(new DailyUsageMetric(from, 1, 2, 1, 0, 30, 45));
        List<RetentionCohortMetric> cohorts = List.of(new RetentionCohortMetric(from, 2, 1, 1, 0, 0, 0));
        when(repository.getSummary(start, endExclusive)).thenReturn(summary);
        when(repository.getDailyActivity(start, endExclusive)).thenReturn(daily);
        when(repository.getRetentionCohorts(start, endExclusive)).thenReturn(cohorts);

        SystemUsageMetricsResponse result = new SystemUsageMetricsServiceImpl(repository).getMetrics(from, to);

        assertEquals(from, result.from());
        assertEquals(to, result.to());
        assertEquals(summary, result.summary());
        assertEquals(daily, result.daily());
        assertEquals(cohorts, result.retentionCohorts());
        verify(repository).getSummary(start, endExclusive);
        verify(repository).getDailyActivity(start, endExclusive);
        verify(repository).getRetentionCohorts(start, endExclusive);
    }

    @Test
    void rejectsInvalidDateRanges() {
        SystemUsageMetricsServiceImpl service = new SystemUsageMetricsServiceImpl(repository);

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
                2, 1, 1, 1, 0, 0, 0, 0, 99, 45, null);
        List<DailyUsageMetric> daily = List.of(new DailyUsageMetric(from, 1, 1, 0, 0, 0, 0));
        when(repository.getUserSummary(42L, start, endExclusive)).thenReturn(summary);
        when(repository.getUserDailyActivity(42L, start, endExclusive)).thenReturn(daily);

        UserUsageMetricsResponse result = new SystemUsageMetricsServiceImpl(repository).getUserMetrics(42L, from, to);

        assertEquals(42L, result.userId());
        assertEquals(summary, result.summary());
        assertEquals(daily, result.daily());
        verify(repository).getUserSummary(42L, start, endExclusive);
        verify(repository).getUserDailyActivity(42L, start, endExclusive);
    }
}
