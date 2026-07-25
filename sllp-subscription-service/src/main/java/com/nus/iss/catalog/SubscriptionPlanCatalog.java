package com.nus.iss.catalog;

import java.util.List;

import org.springframework.stereotype.Component;

import com.nus.iss.dto.SubscriptionPlanInfoDTO;
import com.nus.iss.enums.SubscriptionPlan;

/**
 * Single source of truth for the available subscription plan metadata (display
 * name, price, feature list). Separating this from {@code SubscriptionService}
 * means plan-catalogue changes never require touching state-management logic.
 */
@Component
public class SubscriptionPlanCatalog {

    private static final List<SubscriptionPlanInfoDTO> PLANS = List.of(
            new SubscriptionPlanInfoDTO(
                    SubscriptionPlan.FREE_TRIAL,
                    "Free Trial",
                    "$0",
                    null,
                    List.of("20 compositions per month", "Basic AI feedback", "Standard support")
            ),
            new SubscriptionPlanInfoDTO(
                    SubscriptionPlan.BASIC,
                    "Basic",
                    "$9.99/month",
                    30,
                    List.of("Unlimited compositions", "Detailed AI feedback",
                            "Document uploads", "Standard support")
            ),
            new SubscriptionPlanInfoDTO(
                    SubscriptionPlan.PREMIUM,
                    "Premium",
                    "$29.99/month",
                    30,
                    List.of("Everything in Basic", "Priority AI processing",
                            "Advanced analytics", "Priority support")
            )
    );

    public List<SubscriptionPlanInfoDTO> getAll() {
        return PLANS;
    }
}
