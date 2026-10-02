package com.nus.iss.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.metadata.Usage;

import com.nus.iss.config.SllpProperties;
import com.nus.iss.dto.AiUsage;
import com.nus.iss.service.impl.AiUsageCostCalculatorImpl;

class AiUsageCostCalculatorTest {

    @Test
    void recordsProviderTokensAndCalculatesCostFromTheVersionedPriceSnapshot() {
        AiUsage usage = new AiUsageCostCalculatorImpl(new SllpProperties()).from(new Usage() {
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

        assertEquals(1_000_000L, usage.getPromptTokens());
        assertEquals(200_000L, usage.getCachedInputTokens());
        assertEquals(2_000_000L, usage.getTotalTokens());
        assertEquals(582_000L, usage.getEstimatedCostMicros());
        assertEquals("gpt-6-luna", usage.getModel());
        assertEquals("openai-gpt-6-luna-2026-10", usage.getPricingVersion());
    }
}
