package com.nus.iss.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModerationConfig {
    private static final double LENIENT_THRESHOLD = 0.80;

    @Bean("lenientModerationPolicy")
    public ModerationPolicy lenientModerationPolicy() {
        return new ModerationPolicy(LENIENT_THRESHOLD);
    }

    public record ModerationPolicy(double threshold) {

        public ModerationPolicy {
            if (!Double.isFinite(threshold) || threshold < 0 || threshold > 1) {
                throw new IllegalArgumentException("moderation threshold must be between 0 and 1");
            }
        }

        public boolean isFlagged(double score) {
            return score >= threshold;
        }
    }
}
