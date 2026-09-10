package com.nus.iss.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "stripe")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class StripeProperties {
    private String webhookSecret;
    private String paymentLinkBasic;
    private String paymentLinkPremium;

    /** Returns the configured Payment Link for a paid plan code, or {@code null} if none applies. */
    public String paymentLinkFor(String planCode) {
        if (planCode == null) return null;
        return switch (planCode) {
            case "BASIC" -> paymentLinkBasic;
            case "PREMIUM" -> paymentLinkPremium;
            default -> null;
        };
    }
}
