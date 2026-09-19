package com.nus.iss.contracts.grading;

/** Typed student-facing coaching item produced by the Suggestions agent. */
public record ImprovementSuggestion(
        String criterion,
        String priority,
        String recommendation,
        String exampleRevision,
        String learningTip,
        SuggestionEvidence supportingEvidence
) {
}
