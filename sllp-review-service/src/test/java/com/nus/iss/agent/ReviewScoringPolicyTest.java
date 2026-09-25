package com.nus.iss.agent;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReviewScoringPolicyTest {
    @Test
    void acceptsACompleteAuditableReview() {
        assertDoesNotThrow(() -> ReviewScoringPolicy.validate(reviewWithChecks(List.of(
                check("GROUNDING"), check("RUBRIC_COVERAGE"), check("EVIDENCE_TRACEABILITY"), check("ACTIONABILITY")))));
    }

    @Test
    void rejectsAReviewThatSkipsARequiredCheck() {
        assertThrows(IllegalStateException.class, () -> ReviewScoringPolicy.validate(reviewWithChecks(List.of(
                check("GROUNDING"), check("RUBRIC_COVERAGE"), check("ACTIONABILITY")))));
    }

    @Test
    void replacesAModelClaimedScoreWithTheDeterministicScore() {
        ReviewModelResponse modelClaim = new ReviewModelResponse("APPROVE", 0.9, 1,
                List.of(check("GROUNDING"), check("RUBRIC_COVERAGE"), check("EVIDENCE_TRACEABILITY"), check("ACTIONABILITY")), List.of());
        assertEquals(100, ReviewScoringPolicy.canonicalize(modelClaim).qualityScore());
    }

    @Test
    void calculatesARepeatableScoreFromCheckStatuses() {
        List<ReviewModelResponse.ReviewCheck> checks = List.of(check("GROUNDING"),
                new ReviewModelResponse.ReviewCheck("RUBRIC_COVERAGE", "WARN", "One criterion is too brief."),
                check("EVIDENCE_TRACEABILITY"), check("ACTIONABILITY"));
        assertEquals(90, ReviewScoringPolicy.calculateQualityScore(checks));
    }

    @Test
    void rejectsAnApprovalThatContradictsAFailedCheck() {
        ReviewModelResponse invalid = new ReviewModelResponse("APPROVE", 0.9, 90,
                List.of(check("GROUNDING"), check("RUBRIC_COVERAGE"),
                        new ReviewModelResponse.ReviewCheck("EVIDENCE_TRACEABILITY", "FAIL", "The quoted excerpt is absent."),
                        check("ACTIONABILITY")), List.of());
        assertThrows(IllegalStateException.class, () -> ReviewScoringPolicy.validate(invalid));
    }

    @Test
    void rejectsARegradeThatClaimsEveryCheckPassed() {
        ReviewModelResponse invalid = new ReviewModelResponse("REGRADE", 0.9, 90,
                List.of(check("GROUNDING"), check("RUBRIC_COVERAGE"), check("EVIDENCE_TRACEABILITY"), check("ACTIONABILITY")), List.of());
        assertThrows(IllegalStateException.class, () -> ReviewScoringPolicy.validate(invalid));
    }

    private static ReviewModelResponse reviewWithChecks(List<ReviewModelResponse.ReviewCheck> checks) {
        return new ReviewModelResponse("APPROVE", 0.9, 92, checks,
                List.of(new ReviewModelResponse.Finding("Organisation", "The rationale does not cite the composition.")));
    }

    private static ReviewModelResponse.ReviewCheck check(String name) {
        return new ReviewModelResponse.ReviewCheck(name, "PASS", "The check passed.");
    }
}
