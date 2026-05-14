package com.nus.iss.route;

import com.nus.iss.config.JwtAuthenticationFilter;
import com.nus.iss.config.SllpProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class GatewayRoute {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SllpProperties sllpProperties;

    @Autowired
    public GatewayRoute(JwtAuthenticationFilter jwtAuthenticationFilter, SllpProperties sllpProperties) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.sllpProperties = sllpProperties;
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("user_register", r -> r.path("/api/v1/auth/register")
                        .uri(sllpProperties.getUserService()))
                .route("user_activate", r -> r.path("/api/v1/auth/activate")
                        .uri(sllpProperties.getUserService()))
                .route("user_login", r -> r.path("/api/v1/auth/login")
                        .uri(sllpProperties.getUserService()))
                .route("user_forgot-password", r -> r.path("/api/v1/auth/forgot-password")
                        .uri(sllpProperties.getUserService()))
                .route("user_reset-password", r -> r.path("/api/v1/auth/reset-password")
                        .filters(f -> f.filter(jwtAuthenticationFilter.jwtRoleFilter(
                                List.of(""))))
                        .uri(sllpProperties.getUserService()))
                .build();
    }
}
