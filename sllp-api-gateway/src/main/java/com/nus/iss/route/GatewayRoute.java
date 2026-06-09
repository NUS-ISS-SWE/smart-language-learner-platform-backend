package com.nus.iss.route;

import com.nus.iss.config.JwtAuthenticationFilter;
import com.nus.iss.config.SllpProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import java.util.List;

@Configuration
public class GatewayRoute {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SllpProperties sllpProperties;
    private final List<String> allUserList = List.of("ADMIN", "USER_FREE_TRIAL", "USER_BASIC", "USER_PREMIUM");

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
                        .filters(f -> f.filter(jwtAuthenticationFilter.jwtRoleFilter(allUserList)))
                        .uri(sllpProperties.getUserService()))
                .route("composition_create", r -> r.method(HttpMethod.POST)
                        .and()
                        .path("/api/v1/composition")
                        .filters(f -> f.filter(jwtAuthenticationFilter.jwtRoleFilter(allUserList)))
                        .uri(sllpProperties.getCompositionService()))
                .route("composition_list", r -> r.method(HttpMethod.GET)
                        .and()
                        .path("/api/v1/composition")
                        .filters(f -> f.filter(jwtAuthenticationFilter.jwtRoleFilter(allUserList)))
                        .uri(sllpProperties.getCompositionService()))
                .route("composition_get", r -> r.method(HttpMethod.GET)
                        .and()
                        .path("/api/v1/composition/{id}")
                        .filters(f -> f.filter(jwtAuthenticationFilter.jwtRoleFilter(allUserList)))
                        .uri(sllpProperties.getCompositionService()))
                .route("composition_update", r -> r.method(HttpMethod.PUT)
                        .and()
                        .path("/api/v1/composition/{id}")
                        .filters(f -> f.filter(jwtAuthenticationFilter.jwtRoleFilter(allUserList)))
                        .uri(sllpProperties.getCompositionService()))
                .route("composition_delete", r -> r.method(HttpMethod.DELETE)
                        .and()
                        .path("/api/v1/composition/{id}")
                        .filters(f -> f.filter(jwtAuthenticationFilter.jwtRoleFilter(allUserList)))
                        .uri(sllpProperties.getCompositionService()))
                .route("composition_grading", r -> r.method(HttpMethod.POST)
                        .and()
                        .path("/api/v1/composition/{id}/grading")
                        .filters(f -> f.filter(jwtAuthenticationFilter.jwtRoleFilter(allUserList)))
                        .uri(sllpProperties.getCompositionService()))
                .route("composition_generate", r -> r.method(HttpMethod.POST)
                        .and()
                        .path("/api/v1/composition/generate")
                        .filters(f -> f.filter(jwtAuthenticationFilter.jwtRoleFilter(allUserList)))
                        .uri(sllpProperties.getCompositionService()))

                .build();
    }
}
