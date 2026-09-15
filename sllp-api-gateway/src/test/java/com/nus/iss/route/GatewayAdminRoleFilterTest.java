package com.nus.iss.route;

import com.nus.iss.config.JwtAuthenticationFilter;
import com.nus.iss.config.JwtConfig;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GatewayAdminRoleFilterTest {

    @Mock
    private JwtConfig jwtConfig;

    @Mock
    private Claims claims;

    @Mock
    private GatewayFilterChain chain;

    @InjectMocks
    private JwtAuthenticationFilter authenticationFilter;

    @Test
    void adminRoleIsAcceptedAndIdentityHeadersAreForwarded() {
        MockServerWebExchange exchange = exchangeWithToken();
        when(jwtConfig.getClaims("token")).thenReturn(claims);
        when(claims.get("ROLE", String.class)).thenReturn("ADMIN");
        when(claims.getSubject()).thenReturn("alextan");
        when(claims.get("userId", String.class)).thenReturn("3");
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
        GatewayFilter filter = authenticationFilter.jwtRoleFilter(GatewayRoute.ADMIN_ONLY);

        filter.filter(exchange, chain).block();

        ArgumentCaptor<ServerWebExchange> exchangeCaptor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(exchangeCaptor.capture());
        HttpHeaders forwardedHeaders = exchangeCaptor.getValue().getRequest().getHeaders();
        assertEquals("alextan", forwardedHeaders.getFirst("X-Sllp-Username"));
        assertEquals("3", forwardedHeaders.getFirst("X-Sllp-User-Id"));
    }

    @Test
    void nonAdminRoleIsRejected() {
        MockServerWebExchange exchange = exchangeWithToken();
        when(jwtConfig.getClaims("token")).thenReturn(claims);
        when(claims.get("ROLE", String.class)).thenReturn("USER_FREE_TRIAL");
        when(claims.getSubject()).thenReturn("alextan");
        when(claims.get("userId", String.class)).thenReturn("3");
        GatewayFilter filter = authenticationFilter.jwtRoleFilter(GatewayRoute.ADMIN_ONLY);

        filter.filter(exchange, chain).block();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any(ServerWebExchange.class));
    }

    private static MockServerWebExchange exchangeWithToken() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/admin/rubrics")
                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                .build());
    }

    @Test
    void clientUserIdIsRemovedWhenTokenHasNoUserId() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/product-guide/conversations")
                .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                .header("X-Sllp-User-Id", "999")
                .header("X-Sllp-Username", "forged")
                .build());
        when(jwtConfig.getClaims("token")).thenReturn(claims);
        when(claims.get("ROLE", String.class)).thenReturn("ADMIN");
        when(claims.getSubject()).thenReturn("alextan");
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
        authenticationFilter.jwtRoleFilter(GatewayRoute.ADMIN_ONLY).filter(exchange, chain).block();
        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        assertEquals(null, captor.getValue().getRequest().getHeaders().getFirst("X-Sllp-User-Id"));
        assertEquals("alextan", captor.getValue().getRequest().getHeaders().getFirst("X-Sllp-Username"));
    }
}
