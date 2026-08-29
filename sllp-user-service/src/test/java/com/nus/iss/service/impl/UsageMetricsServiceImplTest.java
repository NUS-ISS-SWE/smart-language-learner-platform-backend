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
import com.nus.iss.repository.UsageMetricsRepository;

@ExtendWith(MockitoExtension.class)
class UsageMetricsServiceImplTest {

    @Mock
    private UsageMetricsRepository repository;

    @Test
    void returnsInclusiveDateRangeAndRepositoryAggregates() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 3);
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime endExclusive = to.plusDays(1).atStartOfDay();
        SystemUsageMetricsResponse.Summary summary = new SystemUsageMetricsResponse.Summary(10, 8, 2, 4, 3, 1, 99);
        List<DailyUsageMetric> daily = List.of(new DailyUsageMetric(from, 1, 2, 1, 0));
        when(repository.summary(start, endExclusive)).thenReturn(summary);
        when(repository.daily(start, endExclusive)).thenReturn(daily);

        SystemUsageMetricsResponse result = new UsageMetricsServiceImpl(repository).getMetrics(from, to);

        assertEquals(from, result.from());
        assertEquals(to, result.to());
        assertEquals(summary, result.summary());
        assertEquals(daily, result.daily());
        verify(repository).summary(start, endExclusive);
        verify(repository).daily(start, endExclusive);
    }

    @Test
    void rejectsAnInvertedOrOverlongDateRange() {
        UsageMetricsServiceImpl service = new UsageMetricsServiceImpl(repository);

        assertThrows(IllegalArgumentException.class,
                () -> service.getMetrics(LocalDate.of(2026, 8, 2), LocalDate.of(2026, 8, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> service.getMetrics(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 1, 2)));
    }
}
