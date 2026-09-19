package com.nus.iss;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.nus.iss.audit.AuditProperties;

@SpringBootApplication
@EnableConfigurationProperties(AuditProperties.class)
public class SllpAdminServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SllpAdminServiceApplication.class, args);
    }
}
