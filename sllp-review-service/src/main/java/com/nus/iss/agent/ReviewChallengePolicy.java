package com.nus.iss.agent;

import java.util.Set;

/** Validates the narrow contract used by the internal adversarial critic. */
public final class ReviewChallengePolicy {
    private static final Set<String> VERDICTS = Set.of("CONFIRM", "CHALLENGE");

    private ReviewChallengePolicy() {}

    public static ReviewChallengeResponse validate(ReviewChallengeResponse result) {
        if (result == null || !VERDICTS.contains(result.verdict()) || blank(result.rationale())) {
            throw new IllegalStateException("invalid adversarial review result");
        }
        if ("CHALLENGE".equals(result.verdict())
                && !ReviewScoringPolicy.REQUIRED_CHECKS.contains(result.checkName())) {
            throw new IllegalStateException("an adversarial challenge must name an audit check");
        }
        return result;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
