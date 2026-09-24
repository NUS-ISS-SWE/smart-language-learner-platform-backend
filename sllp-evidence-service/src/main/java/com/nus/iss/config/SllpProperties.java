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

    private Services services = new Services();
    private String a2aToken;
    private String publicBaseUrl = "http://localhost:8092";

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Services {
        private String compositionServiceUrl;
        private String adminServiceUrl;
        private String userServiceUrl;
    }
}
