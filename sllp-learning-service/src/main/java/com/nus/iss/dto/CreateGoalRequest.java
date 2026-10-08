package com.nus.iss.dto;

import com.nus.iss.enums.GoalMetric;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateGoalRequest(
        @NotNull GoalMetric metric,
        @NotNull @DecimalMin(value = "1.0", message = "Goal target must be positive")
        @DecimalMax(value = "100.0", message = "Score goals cannot exceed 100") BigDecimal target,
        String category,
        LocalDate dueOn) { }
