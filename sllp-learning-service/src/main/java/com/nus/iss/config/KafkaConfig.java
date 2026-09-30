package com.nus.iss.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;
import java.util.HashMap;
import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "sllp.kafka")
public class KafkaConfig {
    private String bootstrapServers;
    private String groupId;
    private Topics topics = new Topics();
    @Data public static class Topics { private String compositionGradingCompleted; }
}
