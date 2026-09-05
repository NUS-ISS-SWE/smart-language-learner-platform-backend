package com.nus.iss.service;

import java.time.LocalDate;

import com.nus.iss.dto.SystemUsageMetricsResponse;
import com.nus.iss.dto.UserUsageMetricsResponse;

public interface SystemUsageMetricsService {
    SystemUsageMetricsResponse getMetrics(LocalDate from, LocalDate to);

    UserUsageMetricsResponse getUserMetrics(long userId, LocalDate from, LocalDate to);
}
