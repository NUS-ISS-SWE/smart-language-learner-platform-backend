package com.nus.iss.agent;

import java.util.List;
import java.util.Set;

/** Deterministic guardrails for the Review Agent's structured assessment. */
public final class ReviewScoringPolicy {
    public static final List<String> REQUIRED_CHECKS = List.of(
            "GROUNDING", "RUBRIC_COVERAGE", "EVIDENCE_TRACEABILITY", "ACTIONABILITY");
    private static final Set<String> DECISIONS = Set.of("APPROVE", "REGRADE", "REJECT");
    private static final Set<String> CHECK_STATUSES = Set.of("PASS", "WARN", "FAIL");

    private ReviewScoringPolicy() {}

    /**
     * Validates the model's audit and replaces its self-reported score with a deterministic one.
     * A model can explain a check, but it cannot award itself a better quality score.
     */
    public static ReviewModelResponse canonicalize(ReviewModelResponse result) {
        if (result == null || !DECISIONS.contains(result.decision()) || !Double.isFinite(result.confidence())
                || result.confidence() < 0 || result.confidence() > 1 || result.checks() == null || result.findings() == null) {
            throw new IllegalStateException("invalid structured review result");
        }

        Set<String> checkNames = new java.util.HashSet<>();
        for (ReviewModelResponse.ReviewCheck check : result.checks()) {
            if (check == null || !REQUIRED_CHECKS.contains(check.name()) || !checkNames.add(check.name())
                    || !CHECK_STATUSES.contains(check.status()) || isBlank(check.rationale())) {
                throw new IllegalStateException("invalid review checks");
            }
        }
        if (!checkNames.equals(Set.copyOf(REQUIRED_CHECKS))) {
            throw new IllegalStateException("review must report every required check");
        }
        for (ReviewModelResponse.Finding finding : result.findings()) {
            if (finding == null || isBlank(finding.message())) {
                throw new IllegalStateException("invalid review findings");
            }
        }
        int qualityScore = calculateQualityScore(result.checks());
        ReviewModelResponse canonical = new ReviewModelResponse(
                result.decision(), result.confidence(), qualityScore, result.checks(), result.findings());
        validateDecisionConsistency(canonical);
        return canonical;
    }

    /** Retained as a concise assertion entry point for policy tests and callers. */
    public static ReviewModelResponse validate(ReviewModelResponse result) {
        return canonicalize(result);
    }

    /** Four equal-weight audit dimensions: PASS=25, WARN=15, FAIL=0 points. */
    public static int calculateQualityScore(List<ReviewModelResponse.ReviewCheck> checks) {
        return checks.stream().mapToInt(check -> switch (check.status()) {
            case "PASS" -> 25;
            case "WARN" -> 15;
            case "FAIL" -> 0;
            default -> throw new IllegalStateException("invalid review check status");
        }).sum();
    }

    /**
     * Prevent a model from approving a result which its own audit says is materially unsound.
     * These rules do not reinterpret the review for the orchestrator; they only reject internally
     * contradictory model output before it crosses the existing A2A boundary.
     */
    private static void validateDecisionConsistency(ReviewModelResponse result) {
        boolean hasFailure = result.checks().stream().anyMatch(check -> "FAIL".equals(check.status()));
        boolean hasConcern = result.checks().stream().anyMatch(check -> !"PASS".equals(check.status()));
        if ("APPROVE".equals(result.decision()) && (hasFailure || result.qualityScore() < 70)) {
            throw new IllegalStateException("an approved review cannot have failed checks or a low quality score");
        }
        if (("REGRADE".equals(result.decision()) || "REJECT".equals(result.decision())) && !hasConcern) {
            throw new IllegalStateException("a non-approval requires at least one review concern");
        }
        if ("REJECT".equals(result.decision()) && !hasFailure) {
            throw new IllegalStateException("a rejected review requires at least one failed check");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
