package com.nus.iss.dto;

import java.math.BigDecimal;

public record CompositionEvaluationResult(
        BigDecimal score,
        String feedback,
        AiUsage usage
) {
    public CompositionEvaluationResult(BigDecimal score, String feedback) {
        this(score, feedback, null);
    }
}
