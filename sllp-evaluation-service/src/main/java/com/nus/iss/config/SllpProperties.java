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
    private Lakera lakera = new Lakera();
    private Services services = new Services();

    @Data
    public static class Services {
        private String userServiceUrl = "http://localhost:8082";
        private String adminServiceUrl = "http://localhost:8086";
    }

    @Data
    public static class Health {
        private URI openAiStatusUrl = URI.create("https://status.openai.com/proxy/status.openai.com");
        private Duration connectTimeout = Duration.ofSeconds(1);
        private Duration readTimeout = Duration.ofSeconds(2);
    }

    @Data
    public static class AiUsage {
        private String provider = "OPENAI";
        private String defaultModel = "gpt-6-luna";
        private String pricingVersion = "openai-gpt-6-luna-2026-10";
        private long inputPriceMicrosPerMillion = 100_000L;
        private long cachedInputPriceMicrosPerMillion = 10_000L;
        private long outputPriceMicrosPerMillion = 500_000L;
    }

    @Data
    public static class Audit {
        private Duration inProgressTimeout = Duration.ofMinutes(2);
    }

    @Data
    public static class Lakera {
        private URI endpoint = URI.create("https://api.lakera.ai/v2/guard");
        private String apiKey = "";
        private Duration connectTimeout = Duration.ofSeconds(5);
        private Duration readTimeout = Duration.ofSeconds(10);
    }
}
