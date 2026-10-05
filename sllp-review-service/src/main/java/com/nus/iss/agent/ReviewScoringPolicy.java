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
        ReviewModelResponse normalized = normalizeNonBlockingActionability(result);
        int qualityScore = calculateQualityScore(normalized.checks());
        ReviewModelResponse canonical = new ReviewModelResponse(
                normalized.decision(), normalized.confidence(), qualityScore, normalized.checks(), normalized.findings());
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
     * Suggestions provides the learner-facing next step after a sound assessment. A missing
     * example in grading feedback is therefore informative, but must not discard an otherwise
     * grounded score or trigger a duplicate grading pass.
     */
    private static ReviewModelResponse normalizeNonBlockingActionability(ReviewModelResponse result) {
        boolean actionabilityConcern = result.checks().stream().anyMatch(check ->
                "ACTIONABILITY".equals(check.name()) && !"PASS".equals(check.status()));
        boolean onlyActionabilityConcern = result.checks().stream().allMatch(check ->
                "ACTIONABILITY".equals(check.name()) || "PASS".equals(check.status()));
        if (!actionabilityConcern || !onlyActionabilityConcern) return result;
        List<ReviewModelResponse.ReviewCheck> checks = result.checks().stream()
                .map(check -> "ACTIONABILITY".equals(check.name()) && "FAIL".equals(check.status())
                        ? new ReviewModelResponse.ReviewCheck(check.name(), "WARN", check.rationale())
                        : check)
                .toList();
        String decision = "APPROVE".equals(result.decision()) ? result.decision() : "APPROVE";
        return new ReviewModelResponse(decision, result.confidence(), result.qualityScore(), checks, result.findings());
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
