package com.nus.iss.dto;

/** Provider-reported token counts plus the immutable cost estimate for one request. */
public record AiUsage(
        long promptTokens,
        long completionTokens,
        long cachedInputTokens,
        long totalTokens,
        long estimatedCostMicros,
        String provider,
        String model,
        String pricingVersion,
        long inputPriceMicrosPerMillion,
        long cachedInputPriceMicrosPerMillion,
        long outputPriceMicrosPerMillion) {
}
