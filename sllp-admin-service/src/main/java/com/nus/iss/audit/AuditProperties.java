package com.nus.iss.audit;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sllp.audit")
public record AuditProperties(String ingestSecret) {
    public AuditProperties {
        if (ingestSecret == null || ingestSecret.isBlank()) {
            ingestSecret = "local-development-audit-secret-change-me";
        }
    }
}
