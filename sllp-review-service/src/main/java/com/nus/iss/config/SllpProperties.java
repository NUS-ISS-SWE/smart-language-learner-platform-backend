package com.nus.iss.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data @Configuration @ConfigurationProperties(prefix = "sllp")
public class SllpProperties {
    private String publicBaseUrl = "http://localhost:8094";
    private String a2aToken;
    /** Development-only switch for exercising downstream A2A peers while Review is being tuned. */
    private boolean reviewAutoApprove = false;
    private String userServiceUrl = "http://localhost:8082";
    private String adminServiceUrl = "http://localhost:8086";
}
