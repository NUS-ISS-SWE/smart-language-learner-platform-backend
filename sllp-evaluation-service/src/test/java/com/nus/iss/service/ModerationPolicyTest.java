package com.nus.iss.service;

import com.nus.iss.config.ModerationConfig.ModerationPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModerationPolicyTest {

    @Test
    void appliesInclusiveLenientThreshold() {
        ModerationPolicy policy = new ModerationPolicy(0.80);

        assertFalse(policy.isFlagged(0.7999));
        assertTrue(policy.isFlagged(0.80));
        assertTrue(policy.isFlagged(0.95));
    }

    @Test
    void rejectsThresholdOutsideScoreRange() {
        assertThrows(IllegalArgumentException.class, () -> new ModerationPolicy(-0.01));
        assertThrows(IllegalArgumentException.class, () -> new ModerationPolicy(1.01));
        assertThrows(IllegalArgumentException.class, () -> new ModerationPolicy(Double.NaN));
    }
}
