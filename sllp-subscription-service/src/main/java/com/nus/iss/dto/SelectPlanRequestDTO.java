package com.nus.iss.dto;

import com.nus.iss.enums.SubscriptionPlan;

import jakarta.validation.constraints.NotNull;

public record SelectPlanRequestDTO(
        @NotNull(message = "plan is required")
        SubscriptionPlan plan,

        /**
         * Optional payment token forwarded by the client.
         * Reserved for the Stripe developer's implementation — pass the Stripe
         * PaymentIntent ID here once the payment flow is wired up.
         */
        String paymentToken
) {}
