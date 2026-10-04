package com.nus.iss.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserSubscriptionTest {

    @Test
    void onCreateSetsTimestampWhenMissing() {
        UserSubscription subscription = new UserSubscription();

        subscription.onCreate();

        assertNotNull(subscription.getCreatedAt());
    }

    @Test
    void onCreatePreservesExistingTimestamp() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 1, 2, 3, 4);
        UserSubscription subscription = UserSubscription.builder().createdAt(timestamp).build();

        subscription.onCreate();

        assertEquals(timestamp, subscription.getCreatedAt());
    }
}
