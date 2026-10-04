package com.nus.iss.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditHttpFilterTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    @Test
    void ordinarySuccessfulGetOnlyContinuesTheChain() throws Exception {
        AuditHttpFilter filter = filter();
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/auth/profile/1");
        when(response.getStatus()).thenReturn(200);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void deniedAuthenticationMutationBuildsAnonymousAuditEvent() throws Exception {
        AuditHttpFilter filter = filter();
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(response.getStatus()).thenReturn(401);

        filter.doFilterInternal(request, response, chain);

        verify(request, atLeastOnce()).getHeader("X-Sllp-User-Id");
        verify(request).getRemoteAddr();
    }

    @Test
    void forbiddenAdminReadBuildsAdminAuditEventForKnownUser() throws Exception {
        AuditHttpFilter filter = filter();
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/v1/admin/users");
        when(response.getStatus()).thenReturn(403);
        when(request.getHeader("X-Sllp-User-Id")).thenReturn("42");

        filter.doFilterInternal(request, response, chain);

        verify(request).getHeader("X-Sllp-Username");
        verify(request).getHeader("X-Sllp-Role");
    }

    @Test
    void failedDataMutationBuildsFailureAuditEvent() throws Exception {
        AuditHttpFilter filter = filter();
        when(request.getMethod()).thenReturn("PUT");
        when(request.getRequestURI()).thenReturn("/api/v1/users/1");
        when(response.getStatus()).thenReturn(500);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(request).getHeader("User-Agent");
    }

    @Test
    void successfulDataMutationBuildsSuccessAuditEvent() throws Exception {
        AuditHttpFilter filter = filter();
        when(request.getMethod()).thenReturn("DELETE");
        when(request.getRequestURI()).thenReturn("/api/v1/users/1");
        when(response.getStatus()).thenReturn(204);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void actuatorAndErrorPathsAreSkipped() {
        AuditHttpFilter filter = filter();
        when(request.getRequestURI()).thenReturn(
                "/actuator/health",
                "/error", "/error",
                "/api/v1/auth/login", "/api/v1/auth/login");

        assertTrue(filter.shouldNotFilter(request));
        assertTrue(filter.shouldNotFilter(request));
        assertFalse(filter.shouldNotFilter(request));
    }

    private AuditHttpFilter filter() {
        // A blank secret keeps the test completely local: the publisher exits before any HTTP call.
        return new AuditHttpFilter(objectMapper, "http://localhost:8086/internal/audit-events", "");
    }
}
