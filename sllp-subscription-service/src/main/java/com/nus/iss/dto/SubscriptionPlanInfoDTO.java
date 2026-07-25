package com.nus.iss.dto;

import java.util.List;

import com.nus.iss.enums.SubscriptionPlan;

public record SubscriptionPlanInfoDTO(
        SubscriptionPlan plan,
        String displayName,
        String price,
        Integer durationDays,
        List<String> features
) {}
