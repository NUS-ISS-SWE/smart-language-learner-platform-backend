package com.nus.iss.agent;

import java.util.List;

/**
 * Structured assessment-review result returned by the model.
 *
 * <p>The decision remains the workflow contract consumed by the orchestrator. The quality score
 * and named checks make the decision auditable: they show whether the assessment is grounded,
 * covers the rubric, traces back to evidence, and gives the student useful next steps.</p>
 */
public record ReviewModelResponse(
        String decision,
        double confidence,
        int qualityScore,
        List<ReviewCheck> checks,
        List<Finding> findings
) {
    public record ReviewCheck(String name, String status, String rationale) {}
    public record Finding(String criterion, String message) {}
}
