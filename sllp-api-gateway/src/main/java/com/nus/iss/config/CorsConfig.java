package com.nus.iss.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Explicit CorsWebFilter bean.
 *
 * Spring Cloud Gateway's spring.cloud.gateway.globalcors YAML config only applies CORS headers
 * when the response flows through the full gateway filter chain. When JwtAuthenticationFilter
 * short-circuits with setComplete() (returning 401/403), the gateway CORS filter never runs,
 * so the preflight response carries no Access-Control-Allow-Origin header and the browser
 * blocks the request.
 *
 * A raw CorsWebFilter bean sits at the WebFlux level, before all gateway filters, so it
 * always writes CORS headers — even on early 401/403 returns.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOriginPatterns(List.of(
            "http://localhost:*",
            "http://*.docker.internal:*",
            "https://nusmtechswe.com"
        ));
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        cors.addAllowedHeader("*");
        cors.setAllowCredentials(true);
        cors.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);

        return new CorsWebFilter(source);
    }
}
