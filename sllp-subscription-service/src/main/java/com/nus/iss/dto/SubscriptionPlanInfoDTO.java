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
        int displayOrder,
        int monthlyAssessmentLimit
) {
    public SubscriptionPlanInfoDTO(String code, String displayName, String description, long priceMinor, String currency,
                                   String billingInterval, Integer durationDays, List<String> features, boolean active,
                                   int displayOrder) {
        this(code, displayName, description, priceMinor, currency, billingInterval, durationDays, features, active, displayOrder, 20);
    }
}
