package com.nus.iss.service;

import java.time.LocalDate;

import com.nus.iss.dto.SystemUsageMetricsResponse;

public interface UsageMetricsService {
    SystemUsageMetricsResponse getMetrics(LocalDate from, LocalDate to);
}
