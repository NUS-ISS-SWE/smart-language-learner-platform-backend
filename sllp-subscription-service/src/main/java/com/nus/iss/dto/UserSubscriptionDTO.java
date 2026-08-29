package com.nus.iss.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.nus.iss.enums.SubscriptionStatus;

public record UserSubscriptionDTO(
        Long id,
        SubscriptionPlanSnapshotDTO plan,
        SubscriptionStatus status,
        LocalDate startDate,
        LocalDate endDate,
        Integer durationDays,
        LocalDateTime createdAt
) {}
