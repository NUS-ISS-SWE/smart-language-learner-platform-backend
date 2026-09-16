package com.nus.iss.agent;

import java.util.List;

/** Shape the model's structured output is bound to directly — not the public DTO shape. */
public record EvidenceAgentResponse(
        List<String> plan,
        List<CriterionFinding> criteria) {

    public record CriterionFinding(String criterion, List<ExcerptFinding> evidence) {
    }

    public record ExcerptFinding(String excerpt, String location) {
    }
}
