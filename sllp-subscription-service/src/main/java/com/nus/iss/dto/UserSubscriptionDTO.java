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
        LocalDateTime createdAt,
        /** Set only when {@code status} is PENDING — where the frontend should redirect
         *  the user to complete payment on Stripe's hosted checkout page. */
        String checkoutUrl
) {}
