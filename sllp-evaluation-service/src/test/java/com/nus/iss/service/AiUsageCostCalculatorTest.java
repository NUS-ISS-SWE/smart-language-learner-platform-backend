package com.nus.iss.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.metadata.Usage;

import com.nus.iss.config.AiUsageProperties;
import com.nus.iss.dto.AiUsage;

class AiUsageCostCalculatorTest {

    @Test
    void recordsProviderTokensAndCalculatesCostFromTheVersionedPriceSnapshot() {
        AiUsage usage = new AiUsageCostCalculator(new AiUsageProperties()).from(new Usage() {
            @Override
            public Integer getPromptTokens() {
                return 1_000_000;
            }

            @Override
            public Integer getCompletionTokens() {
                return 1_000_000;
            }

            @Override
            public Integer getTotalTokens() {
                return 2_000_000;
            }

            @Override
            public Long getCacheReadInputTokens() {
                return 200_000L;
            }

            @Override
            public Object getNativeUsage() {
                return null;
            }
        }, null);

        assertEquals(1_000_000L, usage.promptTokens());
        assertEquals(200_000L, usage.cachedInputTokens());
        assertEquals(2_000_000L, usage.totalTokens());
        assertEquals(735_000L, usage.estimatedCostMicros());
        assertEquals("gpt-4o-mini", usage.model());
        assertEquals("openai-gpt-4o-mini-2026-08", usage.pricingVersion());
    }
}
