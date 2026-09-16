package com.nus.iss.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.ai.chat.metadata.Usage;
import org.springframework.stereotype.Service;

import com.nus.iss.config.AiUsageProperties;
import com.nus.iss.dto.AiUsage;

@Service
public class AiUsageCostCalculator {
    private static final BigDecimal ONE_MILLION = BigDecimal.valueOf(1_000_000L);

    private final AiUsageProperties properties;

    public AiUsageCostCalculator(AiUsageProperties properties) {
        this.properties = properties;
    }

    public AiUsage from(Usage usage, String responseModel) {
        long promptTokens = numberOrZero(usage == null ? null : usage.getPromptTokens());
        long completionTokens = numberOrZero(usage == null ? null : usage.getCompletionTokens());
        long cachedInputTokens = longOrZero(usage == null ? null : usage.getCacheReadInputTokens());
        long totalTokens = numberOrZero(usage == null ? null : usage.getTotalTokens());
        if (totalTokens == 0) totalTokens = promptTokens + completionTokens;

        long nonCachedPromptTokens = Math.max(0L, promptTokens - cachedInputTokens);
        long cost = cost(nonCachedPromptTokens, properties.getInputPriceMicrosPerMillion())
                + cost(cachedInputTokens, properties.getCachedInputPriceMicrosPerMillion())
                + cost(completionTokens, properties.getOutputPriceMicrosPerMillion());
        return new AiUsage(promptTokens, completionTokens, cachedInputTokens, totalTokens, cost,
                properties.getProvider(), blankToDefault(responseModel, properties.getDefaultModel()),
                properties.getPricingVersion(), properties.getInputPriceMicrosPerMillion(), properties.getCachedInputPriceMicrosPerMillion(),
                properties.getOutputPriceMicrosPerMillion());
    }

    private static long cost(long tokens, long microsPerMillion) {
        return BigDecimal.valueOf(tokens).multiply(BigDecimal.valueOf(microsPerMillion))
                .divide(ONE_MILLION, 0, RoundingMode.HALF_UP).longValueExact();
    }

    private static long numberOrZero(Integer value) {
        return value == null ? 0L : value.longValue();
    }

    private static long longOrZero(Long value) {
        return value == null ? 0L : value;
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
