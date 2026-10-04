package com.nus.iss.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModerationConfigTest {

    @Test
    void createsLenientPolicyAtExpectedThreshold() {
        ModerationConfig.ModerationPolicy policy = new ModerationConfig().lenientModerationPolicy();

        assertEquals(0.80, policy.threshold());
        assertFalse(policy.isFlagged(0.79));
        assertTrue(policy.isFlagged(0.80));
    }
}
