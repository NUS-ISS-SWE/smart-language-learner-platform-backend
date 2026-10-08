package com.nus.iss.dto;

import com.nus.iss.enums.GoalMetric;
import com.nus.iss.enums.GoalStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record GoalResponse(Long id, GoalMetric metric, String category, boolean focused, BigDecimal target, BigDecimal progress,
                           GoalStatus status, LocalDate startsOn, LocalDate dueOn,
                           LocalDateTime createdAt, LocalDateTime completedAt) { }
