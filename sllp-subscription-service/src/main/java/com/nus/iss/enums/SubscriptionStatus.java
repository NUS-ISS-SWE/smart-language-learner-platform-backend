package com.nus.iss.enums;

public enum SubscriptionStatus {
    ACTIVE,
    EXPIRED,
    CANCELLED,
    /** Reserved for Stripe webhook confirmation flow — not used in simulated payment path. */
    PENDING
}
