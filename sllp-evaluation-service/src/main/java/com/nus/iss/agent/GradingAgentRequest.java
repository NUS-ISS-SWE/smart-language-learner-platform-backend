package com.nus.iss.agent;

import com.nus.iss.contracts.grading.CompositionGradingRequestedEvent;
import com.nus.iss.enums.Language;

import java.math.BigDecimal;
import java.util.List;

public record GradingAgentRequest(
        Long userId,
        String title,
        String content,
        Language language,
        RubricSnapshot rubricSnapshot,
        List<String> evidence
) {

    public record RubricSnapshot(String version, List<Criterion> criteria) {
        public record Criterion(String name, BigDecimal weight, String guidance) {}
    }

    public GradingAgentRequest {
        if (rubricSnapshot == null || rubricSnapshot.version() == null || rubricSnapshot.version().isBlank()
                || rubricSnapshot.criteria() == null || rubricSnapshot.criteria().isEmpty()) {
            throw new IllegalArgumentException("an immutable rubric snapshot is required");
        }
        rubricSnapshot = new RubricSnapshot(rubricSnapshot.version(), List.copyOf(rubricSnapshot.criteria()));
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }

    public static GradingAgentRequest from(CompositionGradingRequestedEvent event) {
        return new GradingAgentRequest(
                event.getUserId(),
                event.getTitle(),
                event.getContent(),
                Language.valueOf(event.getLanguage()),
                new RubricSnapshot("legacy-rubric", List.of(new RubricSnapshot.Criterion("Legacy score", new java.math.BigDecimal("100"), "Legacy grading"))),
                List.of());
    }
}
