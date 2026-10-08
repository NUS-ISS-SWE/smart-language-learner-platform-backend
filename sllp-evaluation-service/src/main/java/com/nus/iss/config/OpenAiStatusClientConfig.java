package com.nus.iss.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class OpenAiStatusClientConfig {

    @Bean
    RestClient openAiStatusRestClient(RestClient.Builder builder, SllpProperties properties) {
        SllpProperties.Health health = properties.getHealth();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(health.getConnectTimeout());
        requestFactory.setReadTimeout(health.getReadTimeout());

        return builder
                .requestFactory(requestFactory)
                .baseUrl(health.getOpenAiStatusUrl().toString())
                .build();
    }
}
