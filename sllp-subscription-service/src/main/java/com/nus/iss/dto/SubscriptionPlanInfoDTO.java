package com.nus.iss.dto;

import java.util.List;

public record SubscriptionPlanInfoDTO(
        String code,
        String displayName,
        String description,
        long priceMinor,
        String currency,
        String billingInterval,
        Integer durationDays,
        List<String> features,
        boolean active,
        int displayOrder
) {}
