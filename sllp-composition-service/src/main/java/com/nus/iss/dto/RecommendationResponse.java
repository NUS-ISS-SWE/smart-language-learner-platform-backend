package com.nus.iss.dto;

import java.math.BigDecimal;

/** Recommendation for learning content based on user performance. */
public record RecommendationResponse(
        Long id,
        String recommendationType,
        String recommendation,
        BigDecimal performanceScore,
        String reasoning) {
}
