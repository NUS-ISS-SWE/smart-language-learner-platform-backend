package com.nus.iss.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GradingReliabilityConfigurationTest {
    @Test
    void omitsUnsupportedTemperatureForReasoningModel() throws IOException {
        String applicationYaml = new ClassPathResource("application.yaml")
                .getContentAsString(StandardCharsets.UTF_8);

        assertFalse(applicationYaml.contains("temperature:"));
        assertTrue(applicationYaml.contains("model: ${OPENAI_CHAT_MODEL:gpt-6-luna}"));
        assertTrue(applicationYaml.contains("reasoning-effort: none"));
    }
}
