package com.nus.iss.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data @Component @ConfigurationProperties(prefix = "sllp.ai-usage")
public class AiUsageProperties {
    private String provider = "OPENAI";
    private String pricingVersion = "openai-gpt-4o-mini-2026-08";
    private long inputPriceMicrosPerMillion = 150000;
    private long cachedInputPriceMicrosPerMillion = 75000;
    private long outputPriceMicrosPerMillion = 600000;
}
