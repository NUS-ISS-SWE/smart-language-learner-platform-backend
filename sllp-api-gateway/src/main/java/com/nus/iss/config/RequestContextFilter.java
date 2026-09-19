package com.nus.iss.config;

import java.util.List;
import java.util.UUID;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

/** Creates trusted request context and prevents callers from spoofing internal identity headers. */
@Component
public class RequestContextFilter implements GlobalFilter, Ordered {
    private static final List<String> INTERNAL_HEADERS = List.of(
            "X-Sllp-Username", "X-Sllp-Role", "X-Sllp-User-Id", "X-Sllp-Request-Id", "X-Sllp-Correlation-Id");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestId = UUID.randomUUID().toString();
        ServerHttpRequest request = exchange.getRequest().mutate().headers(headers -> {
            INTERNAL_HEADERS.forEach(headers::remove);
            headers.set("X-Sllp-Request-Id", requestId);
        }).build();
        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override public int getOrder() { return Ordered.HIGHEST_PRECEDENCE; }
}
