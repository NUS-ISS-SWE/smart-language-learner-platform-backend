package com.nus.iss.config;

import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JwtAuthenticationFilter {
    private final JwtConfig jwtConfig;

    @Autowired
    public JwtAuthenticationFilter(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    public GatewayFilter jwtRoleFilter(List<String> allowedRoles) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String token = authHeader.substring(7);
            Claims claims;
            try {
                claims = jwtConfig.getClaims(token);
            } catch (Exception e) {
                log.warn("JWT validation failed: {}", e.getMessage());
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String role = claims.get("ROLE", String.class);
            String username = claims.getSubject();
            String userId = claims.get("userId", String.class);

            if (role == null || !allowedRoles.contains(role)) {
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate()
                    .headers(headers -> {
                        headers.remove("X-Sllp-Username");
                        headers.remove("X-Sllp-Role");
                        headers.remove("X-Sllp-User-Id");
                    })
                    .header("X-Sllp-Username", username)
                    .header("X-Sllp-Role", role);
            if (userId != null) {
                requestBuilder.header("X-Sllp-User-Id", userId);
            }
            ServerHttpRequest mutatedRequest = requestBuilder.build();

            ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();
            return chain.filter(mutatedExchange);
        };
    }

}

