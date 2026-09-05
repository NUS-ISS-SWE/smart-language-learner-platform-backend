package com.nus.iss.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/** Versioned price snapshot used when recording an AI provider response. */
@Data
@Component
@ConfigurationProperties(prefix = "sllp.ai-usage")
public class AiUsageProperties {
    private String provider = "OPENAI";
    private String defaultModel = "gpt-4o-mini";
    private String pricingVersion = "openai-gpt-4o-mini-2026-08";
    private long inputPriceMicrosPerMillion = 150_000L;
    private long cachedInputPriceMicrosPerMillion = 75_000L;
    private long outputPriceMicrosPerMillion = 600_000L;
}
