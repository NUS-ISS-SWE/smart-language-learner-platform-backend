package com.nus.iss.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReviewChallengePolicyTest {
    @Test
    void acceptsAConfirmingCritic() {
        assertDoesNotThrow(() -> ReviewChallengePolicy.validate(
                new ReviewChallengeResponse("CONFIRM", null, "The approval is supported by the supplied assessment.")));
    }

    @Test
    void acceptsATraceableChallenge() {
        assertDoesNotThrow(() -> ReviewChallengePolicy.validate(new ReviewChallengeResponse(
                "CHALLENGE", "GROUNDING", "The rationale contains a claim absent from the composition.")));
    }

    @Test
    void rejectsAChallengeWithoutAnAuditDimension() {
        assertThrows(IllegalStateException.class, () -> ReviewChallengePolicy.validate(new ReviewChallengeResponse(
                "CHALLENGE", "STYLE", "The explanation is too general.")));
    }
}
