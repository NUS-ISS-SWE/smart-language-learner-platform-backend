package com.nus.iss.dto;

import java.math.BigDecimal;

public record CompositionEvaluationResult(
        BigDecimal score,
        String feedback
) {
}
