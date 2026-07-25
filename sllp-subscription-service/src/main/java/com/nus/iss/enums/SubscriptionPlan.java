package com.nus.iss.enums;

public enum SubscriptionPlan {
    FREE_TRIAL("USER_FREE_TRIAL", null),
    BASIC("USER_BASIC", 30),
    PREMIUM("USER_PREMIUM", 30);

    private final String role;
    private final Integer durationDays;

    SubscriptionPlan(String role, Integer durationDays) {
        this.role = role;
        this.durationDays = durationDays;
    }

    /** The application role assigned when this plan is activated. */
    public String roleValue() {
        return role;
    }

    /** Subscription length in days, or {@code null} for plans with no expiry (FREE_TRIAL). */
    public Integer durationDays() {
        return durationDays;
    }
}
