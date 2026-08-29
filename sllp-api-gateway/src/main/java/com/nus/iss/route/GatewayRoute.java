package com.nus.iss.route;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import com.nus.iss.config.JwtAuthenticationFilter;
import com.nus.iss.config.SllpProperties;

@Configuration
public class GatewayRoute {
    static final List<String> ADMIN_ONLY = List.of("ADMIN");

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
                .route("user_register", r -> r.method(HttpMethod.POST)
                        .and()
                        .path("/api/v1/auth/register")
                        .uri(sllpProperties.getUserService()))
                .route("user_activate", r -> r.path("/api/v1/auth/activate")
                        .uri(sllpProperties.getUserService()))
                .route("user_login", r -> r.path("/api/v1/auth/login")
                        .uri(sllpProperties.getUserService()))
                .route("user_forgot-password", r -> r.path("/api/v1/auth/forgot-password")
                        .uri(sllpProperties.getUserService()))
                .route("user_forgot-password-confirm", r -> r.path("/api/v1/auth/forgot-password/confirm")
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

                // Document service routes
                .route("document_upload", r -> r.method(HttpMethod.POST)
                        .and()
                        .path("/api/documents/upload")
                        .filters(f -> f.filter(jwtAuthenticationFilter.jwtRoleFilter(allUserList)))
                        .uri(sllpProperties.getDocumentService()))
                .route("document_list", r -> r.method(HttpMethod.GET)
                        .and()
                        .path("/api/documents")
                        .filters(f -> f.filter(jwtAuthenticationFilter.jwtRoleFilter(allUserList)))
                        .uri(sllpProperties.getDocumentService()))
                .route("document_content", r -> r.method(HttpMethod.GET)
                        .and()
                        .path("/api/documents/{id}/content")
                        .filters(f -> f.filter(jwtAuthenticationFilter.jwtRoleFilter(allUserList)))
                        .uri(sllpProperties.getDocumentService()))
                .route("document_get", r -> r.method(HttpMethod.GET)
                        .and()
                        .path("/api/documents/{id}")
                        .filters(f -> f.filter(jwtAuthenticationFilter.jwtRoleFilter(allUserList)))
                        .uri(sllpProperties.getDocumentService()))
                .route("document_delete", r -> r.method(HttpMethod.DELETE)
                        .and()
                        .path("/api/documents/{id}")
                        .filters(f -> f.filter(jwtAuthenticationFilter.jwtRoleFilter(allUserList)))
                        .uri(sllpProperties.getDocumentService()))

                // Subscription service routes
                .route("subscription_plans", r -> r.method(HttpMethod.GET)
                        .and()
                        .path("/api/v1/subscription/plans")
                        .uri(sllpProperties.getSubscriptionService()))
                .route("subscription_current", r -> r.method(HttpMethod.GET)
                        .and()
                        .path("/api/v1/subscription/current")
                        .filters(f -> f.filter(jwtAuthenticationFilter.jwtRoleFilter(allUserList)))
                        .uri(sllpProperties.getSubscriptionService()))
                .route("subscription_select", r -> r.method(HttpMethod.POST)
                        .and()
                        .path("/api/v1/subscription/select")
                        .filters(f -> f.filter(jwtAuthenticationFilter.jwtRoleFilter(allUserList)))
                        .uri(sllpProperties.getSubscriptionService()))
                .route("subscription_cancel", r -> r.method(HttpMethod.POST)
                        .and()
                        .path("/api/v1/subscription/cancel")
                        .filters(f -> f.filter(jwtAuthenticationFilter.jwtRoleFilter(allUserList)))
                        .uri(sllpProperties.getSubscriptionService()))
                .route("subscription_history", r -> r.method(HttpMethod.GET)
                        .and()
                        .path("/api/v1/subscription/history")
                        .filters(f -> f.filter(jwtAuthenticationFilter.jwtRoleFilter(allUserList)))
                        .uri(sllpProperties.getSubscriptionService()))

                // Admin rubric routes
                .route("admin_rubrics", r -> r.path(
                                "/api/v1/admin/rubrics",
                                "/api/v1/admin/rubrics/**")
                        .filters(f -> f.filter(jwtAuthenticationFilter.jwtRoleFilter(ADMIN_ONLY)))
                        .uri(sllpProperties.getAdminService()))
                // Admin routes — ADMIN role only
                .route("admin_usage_metrics", r -> r.method(HttpMethod.GET)
                        .and()
                        .path("/api/v1/admin/metrics")
                        .filters(f -> f.filter(jwtAuthenticationFilter.jwtRoleFilter(ADMIN_ONLY)))
                        .uri(sllpProperties.getUserService()))
                .route("admin_user_list", r -> r.method(HttpMethod.GET)
                        .and()
                        .path("/api/v1/admin/users")
                        .filters(f -> f.filter(jwtAuthenticationFilter.jwtRoleFilter(ADMIN_ONLY)))
                        .uri(sllpProperties.getUserService()))
                .route("admin_user_get", r -> r.method(HttpMethod.GET)
                        .and()
                        .path("/api/v1/admin/users/{id}")
                        .filters(f -> f.filter(jwtAuthenticationFilter.jwtRoleFilter(ADMIN_ONLY)))
                        .uri(sllpProperties.getUserService()))
                .route("admin_user_status", r -> r.method(HttpMethod.PATCH)
                        .and()
                        .path("/api/v1/admin/users/{id}/status")
                        .filters(f -> f.filter(jwtAuthenticationFilter.jwtRoleFilter(ADMIN_ONLY)))
                        .uri(sllpProperties.getUserService()))
                .route("admin_user_role", r -> r.method(HttpMethod.PATCH)
                        .and()
                        .path("/api/v1/admin/users/{id}/role")
                        .filters(f -> f.filter(jwtAuthenticationFilter.jwtRoleFilter(ADMIN_ONLY)))
                        .uri(sllpProperties.getUserService()))
                .route("admin_user_delete", r -> r.method(HttpMethod.DELETE)
                        .and()
                        .path("/api/v1/admin/users/{id}")
                        .filters(f -> f.filter(jwtAuthenticationFilter.jwtRoleFilter(ADMIN_ONLY)))
                        .uri(sllpProperties.getUserService()))
                .route("admin_composition_get", r -> r.method(HttpMethod.GET)
                        .and()
                        .path("/api/v1/admin/compositions/{id}")
                        .filters(f -> f.filter(jwtAuthenticationFilter.jwtRoleFilter(ADMIN_ONLY)))
                        .uri(sllpProperties.getCompositionService()))
                .route("admin_compositions", r -> r.method(HttpMethod.GET)
                        .and()
                        .path("/api/v1/admin/compositions")
                        .filters(f -> f.filter(jwtAuthenticationFilter.jwtRoleFilter(ADMIN_ONLY)))
                        .uri(sllpProperties.getCompositionService()))
                .route("admin_document_content", r -> r.method(HttpMethod.GET)
                        .and()
                        .path("/api/documents/admin/{id}/content")
                        .filters(f -> f.filter(jwtAuthenticationFilter.jwtRoleFilter(ADMIN_ONLY)))
                        .uri(sllpProperties.getDocumentService()))
                .route("admin_document_get", r -> r.method(HttpMethod.GET)
                        .and()
                        .path("/api/documents/admin/{id}")
                        .filters(f -> f.filter(jwtAuthenticationFilter.jwtRoleFilter(ADMIN_ONLY)))
                        .uri(sllpProperties.getDocumentService()))
                .route("admin_documents", r -> r.method(HttpMethod.GET)
                        .and()
                        .path("/api/documents/admin")
                        .filters(f -> f.filter(jwtAuthenticationFilter.jwtRoleFilter(ADMIN_ONLY)))
                        .uri(sllpProperties.getDocumentService()))

                .build();
    }
}
