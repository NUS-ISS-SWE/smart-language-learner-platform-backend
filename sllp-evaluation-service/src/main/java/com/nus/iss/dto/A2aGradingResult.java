package com.nus.iss.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** SLLP A2A v2 grading artifact. */
public record A2aGradingResult(
        BigDecimal overallScore,
        Map<String, BigDecimal> rubricScores,
        List<CriterionAssessment> criteria,
        double confidence,
        String modelVersion,
        String promptVersion,
        AiUsage usage) {
    public record CriterionAssessment(String criterion, BigDecimal score, String rationale, List<Evidence> evidence) {}
    public record Evidence(String excerpt, String location) {}
}
