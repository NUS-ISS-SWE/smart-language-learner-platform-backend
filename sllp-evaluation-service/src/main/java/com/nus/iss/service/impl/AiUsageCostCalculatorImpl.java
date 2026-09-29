package com.nus.iss.service.impl;

import com.nus.iss.config.SllpProperties;
import com.nus.iss.dto.AiUsage;
import com.nus.iss.service.AiUsageCostCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class AiUsageCostCalculatorImpl implements AiUsageCostCalculator {
    private static final BigDecimal ONE_MILLION = BigDecimal.valueOf(1_000_000L);
    private final SllpProperties properties;

    @Override
    public AiUsage from(Usage usage, String responseModel) {
        if (usage == null) {
            throw new IllegalStateException("AI response did not include usage metadata");
        }
        long prompt = numberOrZero(usage.getPromptTokens());
        long completion = numberOrZero(usage.getCompletionTokens());
        long cached = longOrZero(usage.getCacheReadInputTokens());
        long total = numberOrZero(usage.getTotalTokens());
        if (cached > prompt || prompt < 0 || completion < 0 || total < prompt + completion) {
            throw new IllegalStateException("AI response included invalid usage metadata");
        }

        SllpProperties.AiUsage pricing = properties.getAiUsage();
        long estimatedCost = cost(prompt - cached, pricing.getInputPriceMicrosPerMillion())
                + cost(cached, pricing.getCachedInputPriceMicrosPerMillion())
                + cost(completion, pricing.getOutputPriceMicrosPerMillion());
        return AiUsage.builder()
                .promptTokens(prompt).completionTokens(completion).cachedInputTokens(cached).totalTokens(total)
                .estimatedCostMicros(estimatedCost).provider(pricing.getProvider())
                .model(blankToDefault(responseModel, pricing.getDefaultModel()))
                .pricingVersion(pricing.getPricingVersion())
                .inputPriceMicrosPerMillion(pricing.getInputPriceMicrosPerMillion())
                .cachedInputPriceMicrosPerMillion(pricing.getCachedInputPriceMicrosPerMillion())
                .outputPriceMicrosPerMillion(pricing.getOutputPriceMicrosPerMillion()).build();
    }

    private static long cost(long tokens, long rate) {
        return BigDecimal.valueOf(tokens).multiply(BigDecimal.valueOf(rate))
                .divide(ONE_MILLION, 0, RoundingMode.HALF_UP).longValueExact();
    }

    private static long numberOrZero(Integer value) { return value == null ? 0L : value.longValue(); }
    private static long longOrZero(Long value) { return value == null ? 0L : value; }
    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
