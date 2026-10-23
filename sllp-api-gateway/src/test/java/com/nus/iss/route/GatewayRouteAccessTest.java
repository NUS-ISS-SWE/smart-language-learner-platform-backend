package com.nus.iss.route;

import com.nus.iss.config.GatewayAuditPublisher;
import com.nus.iss.config.SllpProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * The gateway's access policy, checked against the real route table from {@link GatewayRoute}:
 * every endpoint must reach the right backend, and only with the roles it allows. Requests are
 * matched and filtered in-process (no backend is called); tokens are signed with the same
 * secret the gateway verifies with.
 */
@SpringBootTest
class GatewayRouteAccessTest {

    enum Access { PUBLIC, ANY_USER, ADMIN_ONLY }

    private static final List<String> USER_ROLES = List.of("ADMIN", "USER_FREE_TRIAL", "USER_BASIC", "USER_PREMIUM");

    // Denied requests are audited to admin-service over HTTP; not wanted in a unit test.
    @MockitoBean
    private GatewayAuditPublisher auditPublisher;

    @Autowired
    private RouteLocator routeLocator;

    @Autowired
    private SllpProperties sllpProperties;

    @Value("${sllp.security.jwt.secret}")
    private String jwtSecret;

    /** One representative request per route: method, path, expected route id, backend, access. */
    static Stream<Arguments> routes() {
        Function<SllpProperties, String> user = SllpProperties::getUserService;
        Function<SllpProperties, String> composition = SllpProperties::getCompositionService;
        Function<SllpProperties, String> document = SllpProperties::getDocumentService;
        Function<SllpProperties, String> subscription = SllpProperties::getSubscriptionService;
        Function<SllpProperties, String> admin = SllpProperties::getAdminService;
        Function<SllpProperties, String> productGuide = SllpProperties::getProductGuideService;
        return Stream.of(
                arguments(HttpMethod.POST, "/api/v1/product-guide/conversations", "product_guide", productGuide, Access.ANY_USER),

                arguments(HttpMethod.POST, "/api/v1/auth/register", "user_register", user, Access.PUBLIC),
                arguments(HttpMethod.GET, "/api/v1/auth/activate", "user_activate", user, Access.PUBLIC),
                arguments(HttpMethod.POST, "/api/v1/auth/login", "user_login", user, Access.PUBLIC),
                arguments(HttpMethod.POST, "/api/v1/auth/forgot-password", "user_forgot-password", user, Access.PUBLIC),
                arguments(HttpMethod.POST, "/api/v1/auth/forgot-password/confirm", "user_forgot-password-confirm", user, Access.PUBLIC),
                arguments(HttpMethod.POST, "/api/v1/auth/reset-password", "user_reset-password", user, Access.ANY_USER),

                arguments(HttpMethod.POST, "/api/v1/composition", "composition_create", composition, Access.ANY_USER),
                arguments(HttpMethod.GET, "/api/v1/composition", "composition_list", composition, Access.ANY_USER),
                arguments(HttpMethod.GET, "/api/v1/composition/12", "composition_get", composition, Access.ANY_USER),
                arguments(HttpMethod.PUT, "/api/v1/composition/12", "composition_update", composition, Access.ANY_USER),
                arguments(HttpMethod.DELETE, "/api/v1/composition/12", "composition_delete", composition, Access.ANY_USER),
                arguments(HttpMethod.POST, "/api/v1/composition/12/grading", "composition_grading", composition, Access.ANY_USER),
                arguments(HttpMethod.POST, "/api/v1/composition/generate", "composition_generate", composition, Access.ANY_USER),

                arguments(HttpMethod.POST, "/api/documents/upload", "document_upload", document, Access.ANY_USER),
                arguments(HttpMethod.GET, "/api/documents", "document_list", document, Access.ANY_USER),
                arguments(HttpMethod.GET, "/api/documents/5/content", "document_content", document, Access.ANY_USER),
                arguments(HttpMethod.GET, "/api/documents/5", "document_get", document, Access.ANY_USER),
                arguments(HttpMethod.DELETE, "/api/documents/5", "document_delete", document, Access.ANY_USER),

                arguments(HttpMethod.GET, "/api/v1/subscription/plans", "subscription_plans", subscription, Access.PUBLIC),
                arguments(HttpMethod.GET, "/api/v1/subscription/current", "subscription_current", subscription, Access.ANY_USER),
                arguments(HttpMethod.POST, "/api/v1/subscription/select", "subscription_select", subscription, Access.ANY_USER),
                arguments(HttpMethod.POST, "/api/v1/subscription/cancel", "subscription_cancel", subscription, Access.ANY_USER),
                arguments(HttpMethod.GET, "/api/v1/subscription/history", "subscription_history", subscription, Access.ANY_USER),
                arguments(HttpMethod.POST, "/api/v1/subscription/webhook", "subscription_webhook", subscription, Access.PUBLIC),
                arguments(HttpMethod.PUT, "/api/v1/admin/subscription/plans/3", "admin_subscription_plans", subscription, Access.ADMIN_ONLY),

                arguments(HttpMethod.GET, "/api/v1/admin/rubrics", "admin_rubrics", admin, Access.ADMIN_ONLY),
                arguments(HttpMethod.DELETE, "/api/v1/admin/rubrics/4", "admin_rubrics", admin, Access.ADMIN_ONLY),
                arguments(HttpMethod.GET, "/api/v1/admin/metrics", "admin_usage_metrics", admin, Access.ADMIN_ONLY),
                arguments(HttpMethod.GET, "/api/v1/admin/audit-events", "admin_audit_events", admin, Access.ADMIN_ONLY),
                arguments(HttpMethod.GET, "/api/v1/admin/metrics/users/7", "admin_user_usage_metrics", admin, Access.ADMIN_ONLY),

                arguments(HttpMethod.GET, "/api/v1/admin/users", "admin_user_list", user, Access.ADMIN_ONLY),
                arguments(HttpMethod.GET, "/api/v1/admin/users/7", "admin_user_get", user, Access.ADMIN_ONLY),
                arguments(HttpMethod.PATCH, "/api/v1/admin/users/7/status", "admin_user_status", user, Access.ADMIN_ONLY),
                arguments(HttpMethod.PATCH, "/api/v1/admin/users/7/role", "admin_user_role", user, Access.ADMIN_ONLY),
                arguments(HttpMethod.DELETE, "/api/v1/admin/users/7", "admin_user_delete", user, Access.ADMIN_ONLY),

                arguments(HttpMethod.GET, "/api/v1/admin/compositions/12", "admin_composition_get", composition, Access.ADMIN_ONLY),
                arguments(HttpMethod.GET, "/api/v1/admin/compositions", "admin_compositions", composition, Access.ADMIN_ONLY),

                arguments(HttpMethod.GET, "/api/documents/admin/5/content", "admin_document_content", document, Access.ADMIN_ONLY),
                arguments(HttpMethod.GET, "/api/documents/admin/5", "admin_document_get", document, Access.ADMIN_ONLY),
                arguments(HttpMethod.GET, "/api/documents/admin", "admin_documents", document, Access.ADMIN_ONLY)
        );
    }

    @ParameterizedTest(name = "{0} {1} -> {2} ({4})")
    @MethodSource("routes")
    void routeReachesItsBackendWithOnlyTheAllowedRoles(HttpMethod method, String path, String routeId,
                                                       Function<SllpProperties, String> backend, Access access) {
        Route route = matchRoute(method, path, null);
        assertNotNull(route, "no route matches " + method + " " + path);
        assertEquals(routeId, route.getId());
        assertEquals(URI.create(backend.apply(sllpProperties)), route.getUri());

        switch (access) {
            case PUBLIC -> assertForwarded(method, path, null);
            case ANY_USER -> {
                assertStatus(HttpStatus.UNAUTHORIZED, method, path, null);
                USER_ROLES.forEach(role -> assertForwarded(method, path, role));
                assertStatus(HttpStatus.FORBIDDEN, method, path, "GUEST");
            }
            case ADMIN_ONLY -> {
                assertStatus(HttpStatus.UNAUTHORIZED, method, path, null);
                USER_ROLES.stream().filter(role -> !role.equals("ADMIN"))
                        .forEach(role -> assertStatus(HttpStatus.FORBIDDEN, method, path, role));
                assertForwarded(method, path, "ADMIN");
            }
        }
    }

    @Test
    void everyRouteHasAnAccessRuleInThisTest() {
        Set<String> tested = routes().map(args -> (String) args.get()[2]).collect(Collectors.toSet());
        Set<String> defined = routeLocator.getRoutes().map(Route::getId).collect(Collectors.toSet()).block();
        assertEquals(defined, tested, "add a row to routes() for every new gateway route");
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String forged = Jwts.builder()
                .subject("mallory")
                .claim("ROLE", "ADMIN")
                .signWith(Keys.hmacShaKeyFor("another-secret-that-is-long-enough-for-hmac-sha".getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertRejected(HttpStatus.UNAUTHORIZED, HttpMethod.GET, "/api/v1/admin/users", forged, "forged ADMIN");
    }

    @Test
    void tokenWithoutRoleIsForbidden() {
        String noRole = Jwts.builder()
                .subject("alex")
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertRejected(HttpStatus.FORBIDDEN, HttpMethod.GET, "/api/v1/composition", noRole, "no ROLE claim");
    }

    @Test
    void forwardedRequestCarriesIdentityFromTheToken() {
        ServerWebExchange forwarded = send(HttpMethod.GET, "/api/v1/composition", token("USER_BASIC")).forwarded();

        assertNotNull(forwarded);
        HttpHeaders headers = forwarded.getRequest().getHeaders();
        assertEquals("alex", headers.getFirst("X-Sllp-Username"));
        assertEquals("USER_BASIC", headers.getFirst("X-Sllp-Role"));
        assertEquals("42", headers.getFirst("X-Sllp-User-Id"));
    }

    @Test
    void unknownPathHasNoRoute() {
        assertNull(matchRoute(HttpMethod.GET, "/api/v1/unknown", null));
        // Method matters: the plan list is public for GET only.
        assertNull(matchRoute(HttpMethod.DELETE, "/api/v1/subscription/plans", null));
    }

    // --- helpers ---

    private record Outcome(ServerWebExchange exchange, ServerWebExchange forwarded) {
        HttpStatusCode status() {
            return exchange.getResponse().getStatusCode();
        }
    }

    private String token(String role) {
        return Jwts.builder()
                .subject("alex")
                .claim("ROLE", role)
                .claim("userId", "42")
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    /** role null = no Authorization header. */
    private void assertForwarded(HttpMethod method, String path, String role) {
        Outcome outcome = send(method, path, role == null ? null : token(role));
        assertNotNull(outcome.forwarded(),
                method + " " + path + " as " + (role == null ? "no token" : role) + " should be forwarded, got " + outcome.status());
    }

    private void assertStatus(HttpStatus expected, HttpMethod method, String path, String role) {
        assertRejected(expected, method, path, role == null ? null : token(role), role == null ? "no token" : role);
    }

    private void assertRejected(HttpStatus expected, HttpMethod method, String path, String token, String who) {
        Outcome outcome = send(method, path, token);
        assertNull(outcome.forwarded(), method + " " + path + " as " + who + " should not be forwarded");
        assertEquals(expected, outcome.status(), method + " " + path + " as " + who);
    }

    /** Runs the matched route's filters; the request counts as forwarded if it reaches the end of the chain. */
    private Outcome send(HttpMethod method, String path, String token) {
        MockServerWebExchange exchange = exchange(method, path, token);
        Route route = matchRoute(exchange);
        assertNotNull(route, "no route matches " + method + " " + path);
        List<ServerWebExchange> forwarded = new ArrayList<>();
        runFilters(route.getFilters(), 0, exchange, forwarded).block();
        return new Outcome(exchange, forwarded.isEmpty() ? null : forwarded.getFirst());
    }

    private static Mono<Void> runFilters(List<GatewayFilter> filters, int index, ServerWebExchange exchange,
                                         List<ServerWebExchange> forwarded) {
        if (index == filters.size()) {
            forwarded.add(exchange);
            return Mono.empty();
        }
        return filters.get(index).filter(exchange, next -> runFilters(filters, index + 1, next, forwarded));
    }

    private Route matchRoute(HttpMethod method, String path, String token) {
        return matchRoute(exchange(method, path, token));
    }

    /** First matching route in order, as the gateway's RoutePredicateHandlerMapping picks it. */
    private Route matchRoute(ServerWebExchange exchange) {
        return routeLocator.getRoutes()
                .concatMap(route -> Flux.from(route.getPredicate().apply(exchange))
                        .filter(Boolean::booleanValue)
                        .map(matched -> route))
                .next()
                .block();
    }

    private static MockServerWebExchange exchange(HttpMethod method, String path, String token) {
        MockServerHttpRequest.BaseBuilder<?> request = MockServerHttpRequest.method(method, path);
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return MockServerWebExchange.from(request.build());
    }
}
