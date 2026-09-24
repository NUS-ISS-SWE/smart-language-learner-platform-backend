package com.nus.iss.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Model-only portion of the A2A result; provider usage is attached by the service. */
public record A2aModelGradingResponse(
        BigDecimal overallScore,
        Map<String, BigDecimal> rubricScores,
        List<A2aGradingResult.CriterionAssessment> criteria,
        double confidence) {}
