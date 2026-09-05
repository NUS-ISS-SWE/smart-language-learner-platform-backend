package com.nus.iss.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nus.iss.dto.SystemUsageMetricsResponse;
import com.nus.iss.dto.UserUsageMetricsResponse;
import com.nus.iss.repository.SystemUsageMetricsQueryRepository;
import com.nus.iss.service.SystemUsageMetricsService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SystemUsageMetricsServiceImpl implements SystemUsageMetricsService {

    static final int MAXIMUM_RANGE_DAYS = 366;

    private final SystemUsageMetricsQueryRepository metricsQueryRepository;

    @Override
    @Transactional(readOnly = true)
    public SystemUsageMetricsResponse getMetrics(LocalDate from, LocalDate to) {
        DateRange range = validateRange(from, to);
        return new SystemUsageMetricsResponse(
                from,
                to,
                metricsQueryRepository.getSummary(range.start(), range.endExclusive()),
                metricsQueryRepository.getDailyActivity(range.start(), range.endExclusive()),
                metricsQueryRepository.getRetentionCohorts(range.start(), range.endExclusive()));
    }

    @Override
    @Transactional(readOnly = true)
    public UserUsageMetricsResponse getUserMetrics(long userId, LocalDate from, LocalDate to) {
        if (userId < 1) throw new IllegalArgumentException("User ID must be positive.");
        DateRange range = validateRange(from, to);
        return new UserUsageMetricsResponse(userId, from, to,
                metricsQueryRepository.getUserSummary(userId, range.start(), range.endExclusive()),
                metricsQueryRepository.getUserDailyActivity(userId, range.start(), range.endExclusive()));
    }

    private static DateRange validateRange(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) throw new IllegalArgumentException("The end date must not be before the start date.");
        if (from.plusDays(MAXIMUM_RANGE_DAYS - 1L).isBefore(to)) {
            throw new IllegalArgumentException("Metrics can be requested for at most 366 days.");
        }
        return new DateRange(from.atStartOfDay(), to.plusDays(1).atStartOfDay());
    }

    private record DateRange(LocalDateTime start, LocalDateTime endExclusive) {
    }
}
