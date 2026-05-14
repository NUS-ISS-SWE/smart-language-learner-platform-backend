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
    public String userService;
}
