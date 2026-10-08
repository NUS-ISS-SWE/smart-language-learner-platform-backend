package com.nus.iss.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    void rejectsMissingUsageMetadata() {
        AiUsageCostCalculatorImpl calculator = new AiUsageCostCalculatorImpl(new SllpProperties());

        assertThrows(IllegalStateException.class, () -> calculator.from(null, "model"));
    }

    @Test
    void treatsNullTokenCountsAsZeroAndBlankModelAsDefault() {
        Usage metadata = mock(Usage.class);

        AiUsage result = new AiUsageCostCalculatorImpl(new SllpProperties()).from(metadata, " ");

        assertEquals(0L, result.getPromptTokens());
        assertEquals(0L, result.getCompletionTokens());
        assertEquals(0L, result.getCachedInputTokens());
        assertEquals(0L, result.getTotalTokens());
        assertEquals("gpt-6-luna", result.getModel());
    }

    @Test
    void keepsNonBlankResponseModel() {
        Usage metadata = usage(10, 2, 12, 0L);

        AiUsage result = new AiUsageCostCalculatorImpl(new SllpProperties()).from(metadata, "gpt-test");

        assertEquals("gpt-test", result.getModel());
    }

    @Test
    void rejectsInconsistentTokenMetadata() {
        AiUsageCostCalculatorImpl calculator = new AiUsageCostCalculatorImpl(new SllpProperties());

        assertThrows(IllegalStateException.class, () -> calculator.from(usage(2, 1, 3, 3L), "model"));
        assertThrows(IllegalStateException.class, () -> calculator.from(usage(-1, 0, 0, -2L), "model"));
        assertThrows(IllegalStateException.class, () -> calculator.from(usage(0, -1, 0, 0L), "model"));
        assertThrows(IllegalStateException.class, () -> calculator.from(usage(10, 5, 14, 0L), "model"));
    }

    private static Usage usage(Integer prompt, Integer completion, Integer total, Long cached) {
        Usage usage = mock(Usage.class);
        when(usage.getPromptTokens()).thenReturn(prompt);
        when(usage.getCompletionTokens()).thenReturn(completion);
        when(usage.getTotalTokens()).thenReturn(total);
        when(usage.getCacheReadInputTokens()).thenReturn(cached);
        return usage;
    }
}
