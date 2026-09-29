package com.nus.iss.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "sllp")
@Data
public class SllpProperties {
    private String a2aToken;
    private String publicBaseUrl = "http://localhost:8093";
    private Health health = new Health();
    private AiUsage aiUsage = new AiUsage();
    private Audit audit = new Audit();

    @Data
    public static class Health {
        private URI openAiStatusUrl = URI.create("https://status.openai.com/proxy/status.openai.com");
        private Duration connectTimeout = Duration.ofSeconds(1);
        private Duration readTimeout = Duration.ofSeconds(2);
    }

    @Data
    public static class AiUsage {
        private String provider = "OPENAI";
        private String defaultModel = "gpt-4o-mini";
        private String pricingVersion = "openai-gpt-4o-mini-2026-08";
        private long inputPriceMicrosPerMillion = 150_000L;
        private long cachedInputPriceMicrosPerMillion = 75_000L;
        private long outputPriceMicrosPerMillion = 600_000L;
    }

    @Data
    public static class Audit {
        private Duration inProgressTimeout = Duration.ofMinutes(2);
    }
}
