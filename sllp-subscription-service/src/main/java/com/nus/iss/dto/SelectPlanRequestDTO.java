package com.nus.iss.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SelectPlanRequestDTO(
        @NotBlank(message = "planCode is required")
        @Pattern(regexp = "[A-Z][A-Z0-9_]{1,49}", message = "planCode must be an uppercase stable code")
        String planCode,

        /**
         * Optional payment token forwarded by the client.
         * Reserved for the Stripe developer's implementation — pass the Stripe
         * PaymentIntent ID here once the payment flow is wired up.
         */
        String paymentToken
) {}
