package com.nus.iss.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record GradingScoreResult(
        BigDecimal overallScore,
        Map<String, BigDecimal> rubricScores,
        List<CriterionAssessmentDTO> criteria) {
}
