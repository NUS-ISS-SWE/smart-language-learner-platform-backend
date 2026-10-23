package com.nus.iss.service;

import com.nus.iss.config.AiUsageProperties;
import com.nus.iss.dto.AiUsage;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.metadata.Usage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiUsageCostCalculatorTest {

    // Default price snapshot: input 150_000, cached input 75_000, output 600_000 micros per million tokens.
    private final AiUsageCostCalculator calculator = new AiUsageCostCalculator(new AiUsageProperties());

    @Test
    void costsPromptAndCompletionTokensAtTheirOwnPrice() {
        AiUsage usage = calculator.from(usage(1_000_000, 500_000, null, 1_500_000), "gpt-4o-mini-2026");

        // 1M input * 0.15 + 0.5M output * 0.60 = 150_000 + 300_000 micros
        assertEquals(450_000L, usage.estimatedCostMicros());
        assertEquals(1_000_000L, usage.promptTokens());
        assertEquals(500_000L, usage.completionTokens());
        assertEquals(1_500_000L, usage.totalTokens());
        assertEquals("gpt-4o-mini-2026", usage.model());
        assertEquals("OPENAI", usage.provider());
        assertEquals("openai-gpt-4o-mini-2026-08", usage.pricingVersion());
    }

    @Test
    void cachedPromptTokensAreBilledAtTheCachedPrice() {
        AiUsage usage = calculator.from(usage(1_000_000, 0, 400_000L, 1_000_000), "gpt-4o-mini");

        // 600k uncached * 0.15 + 400k cached * 0.075 = 90_000 + 30_000 micros
        assertEquals(120_000L, usage.estimatedCostMicros());
        assertEquals(400_000L, usage.cachedInputTokens());
    }

    @Test
    void cachedCountLargerThanPromptNeverGoesNegative() {
        AiUsage usage = calculator.from(usage(100, 0, 500L, 100), "gpt-4o-mini");

        // All uncached tokens clamp to 0; the 500 cached tokens cost 37.5 micros, rounded half-up.
        assertEquals(38L, usage.estimatedCostMicros());
    }

    @Test
    void totalIsDerivedWhenTheProviderOmitsIt() {
        AiUsage usage = calculator.from(usage(120, 30, null, null), "gpt-4o-mini");

        assertEquals(150L, usage.totalTokens());
    }

    @Test
    void missingUsageCostsNothingAndFallsBackToTheDefaultModel() {
        AiUsage usage = calculator.from(null, " ");

        assertEquals(0L, usage.totalTokens());
        assertEquals(0L, usage.estimatedCostMicros());
        assertEquals("gpt-4o-mini", usage.model());
    }

    @Test
    void usesTheConfiguredPriceSnapshot() {
        AiUsageProperties properties = new AiUsageProperties();
        properties.setInputPriceMicrosPerMillion(1_000_000L);
        properties.setOutputPriceMicrosPerMillion(2_000_000L);

        AiUsage usage = new AiUsageCostCalculator(properties).from(usage(10, 5, null, 15), null);

        assertEquals(20L, usage.estimatedCostMicros());
        assertEquals(1_000_000L, usage.inputPriceMicrosPerMillion());
        assertEquals(2_000_000L, usage.outputPriceMicrosPerMillion());
    }

    private static Usage usage(Integer prompt, Integer completion, Long cached, Integer total) {
        Usage usage = mock(Usage.class);
        when(usage.getPromptTokens()).thenReturn(prompt);
        when(usage.getCompletionTokens()).thenReturn(completion);
        when(usage.getCacheReadInputTokens()).thenReturn(cached);
        when(usage.getTotalTokens()).thenReturn(total);
        return usage;
    }
}
