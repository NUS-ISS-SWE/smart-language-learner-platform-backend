package com.nus.iss.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "sllp")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class SllpProperties {

    private Kafka kafka = new Kafka();
    private Services services = new Services();
    private String a2aToken;
    private String publicBaseUrl = "http://localhost:8093";

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Services {
        private String userServiceUrl;
        private String adminServiceUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Kafka {
        private String bootstrapServers;
        private Topics topics = new Topics();
        private String groupId;
        private String autoOffsetReset;
        private int retryBackoffMs;
        private int requestTimeoutMs;
        private int defaultApiTimeoutMs;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Topics {
        private String compositionGradingRequested;
        private String compositionGradingCompleted;
    }
}
