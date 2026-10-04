package com.nus.iss.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RubricCriterionTest {

    @Test
    void onCreateSetsMissingTimestamps() {
        RubricCriterion criterion = new RubricCriterion();

        criterion.onCreate();

        assertNotNull(criterion.getCreatedAt());
        assertNotNull(criterion.getUpdatedAt());
    }

    @Test
    void onCreatePreservesExistingTimestamps() {
        LocalDateTime created = LocalDateTime.of(2026, 1, 2, 3, 4);
        LocalDateTime updated = LocalDateTime.of(2026, 2, 3, 4, 5);
        RubricCriterion criterion = RubricCriterion.builder().createdAt(created).updatedAt(updated).build();

        criterion.onCreate();

        assertEquals(created, criterion.getCreatedAt());
        assertEquals(updated, criterion.getUpdatedAt());
    }

    @Test
    void onUpdateRefreshesTimestamp() {
        RubricCriterion criterion = RubricCriterion.builder()
                .updatedAt(LocalDateTime.of(2020, 1, 1, 0, 0)).build();

        criterion.onUpdate();

        assertTrue(criterion.getUpdatedAt().isAfter(LocalDateTime.of(2020, 1, 1, 0, 0)));
    }
}
