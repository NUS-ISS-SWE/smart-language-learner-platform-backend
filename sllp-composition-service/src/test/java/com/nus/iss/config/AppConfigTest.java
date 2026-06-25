package com.nus.iss.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class AppConfigTest {

    @Test
    void jsonMapperBuilderCustomizerBeanCanBeCreated() {
        AppConfig appConfig = new AppConfig();

        assertNotNull(appConfig.jsonMapperBuilderCustomizer());
    }
}
