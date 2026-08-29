package com.nus.iss.service.impl;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nus.iss.dto.SystemUsageMetricsResponse;
import com.nus.iss.repository.UsageMetricsRepository;
import com.nus.iss.service.UsageMetricsService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsageMetricsServiceImpl implements UsageMetricsService {

    static final int MAXIMUM_RANGE_DAYS = 366;

    private final UsageMetricsRepository usageMetricsRepository;

    @Override
    @Transactional(readOnly = true)
    public SystemUsageMetricsResponse getMetrics(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("The end date must not be before the start date.");
        }
        if (from.plusDays(MAXIMUM_RANGE_DAYS - 1L).isBefore(to)) {
            throw new IllegalArgumentException("Metrics can be requested for at most 366 days.");
        }

        return new SystemUsageMetricsResponse(
                from,
                to,
                usageMetricsRepository.summary(from.atStartOfDay(), to.plusDays(1).atStartOfDay()),
                usageMetricsRepository.daily(from.atStartOfDay(), to.plusDays(1).atStartOfDay()));
    }
}
