package com.nus.iss.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class SafeguardClientConfig {

    @Bean
    @Qualifier("SafeguardRestClient")
    RestClient SafeguardRestClient(RestClient.Builder builder, SllpProperties properties) {
        SllpProperties.Lakera lakera = properties.getLakera();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(lakera.getConnectTimeout());
        requestFactory.setReadTimeout(lakera.getReadTimeout());
        return builder.requestFactory(requestFactory).build();
    }
}
