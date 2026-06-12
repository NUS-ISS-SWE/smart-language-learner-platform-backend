package com.nus.iss;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication(scanBasePackages = "com.nus.iss.user")
@EnableJpaAuditing
public class SllpUserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SllpUserServiceApplication.class, args);
    }
}
