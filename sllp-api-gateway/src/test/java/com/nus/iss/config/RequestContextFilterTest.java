package com.nus.iss.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RequestContextFilterTest {

    private final RequestContextFilter filter = new RequestContextFilter();

    @Test
    void identityHeadersSentByTheClientAreDropped() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/composition")
                .header("X-Sllp-Username", "forged")
                .header("X-Sllp-Role", "ADMIN")
                .header("X-Sllp-User-Id", "1")
                .header("X-Sllp-Correlation-Id", "forged-correlation")
                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                .build());

        HttpHeaders forwarded = forward(exchange).getRequest().getHeaders();

        assertNull(forwarded.getFirst("X-Sllp-Username"));
        assertNull(forwarded.getFirst("X-Sllp-Role"));
        assertNull(forwarded.getFirst("X-Sllp-User-Id"));
        assertNull(forwarded.getFirst("X-Sllp-Correlation-Id"));
        // Everything else passes through untouched.
        assertEquals("Bearer token", forwarded.getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void everyRequestGetsItsOwnGatewayRequestId() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/composition")
                .header("X-Sllp-Request-Id", "client-chosen")
                .build());

        String first = forward(exchange).getRequest().getHeaders().getFirst("X-Sllp-Request-Id");
        String second = forward(exchange).getRequest().getHeaders().getFirst("X-Sllp-Request-Id");

        assertNotEquals("client-chosen", first);
        assertDoesNotThrow(() -> UUID.fromString(first));
        assertNotEquals(first, second);
    }

    @Test
    void runsBeforeEveryOtherFilter() {
        assertEquals(Ordered.HIGHEST_PRECEDENCE, filter.getOrder());
    }

    private ServerWebExchange forward(ServerWebExchange exchange) {
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        filter.filter(exchange, next -> {
            forwarded.set(next);
            return Mono.empty();
        }).block();
        return forwarded.get();
    }
}
