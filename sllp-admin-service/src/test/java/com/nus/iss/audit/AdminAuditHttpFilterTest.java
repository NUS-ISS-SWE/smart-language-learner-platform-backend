package com.nus.iss.audit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuditHttpFilterTest {

    @Mock
    private AdminAuditRecorder recorder;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;

    private AdminAuditHttpFilter filter;

    @BeforeEach
    void setUp() {
        filter = new AdminAuditHttpFilter(recorder);
    }

    @Test
    void successfulAdminReadIsRecordedAfterChain() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/admin/users");
        when(request.getHeader("X-Sllp-Username")).thenReturn("ming");
        when(request.getHeader("X-Sllp-User-Id")).thenReturn("42");
        when(request.getHeader("X-Sllp-Request-Id")).thenReturn("request-id");
        when(request.getMethod()).thenReturn("GET");
        when(response.getStatus()).thenReturn(200);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(recorder).record("ADMIN_RESOURCE_VIEWED", "ming", "42", "HTTP_RESOURCE",
                "/api/v1/admin/users", "request-id", "GET", "/api/v1/admin/users");
    }

    @Test
    void failedAdminReadAndOrdinaryPathAreNotRecorded() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/admin/users", "/api/v1/auth/login");
        when(response.getStatus()).thenReturn(500);

        filter.doFilterInternal(request, response, chain);
        filter.doFilterInternal(request, response, chain);

        verify(recorder, never()).record(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void internalAndErrorPathsAreSkipped() {
        when(request.getRequestURI()).thenReturn(
                "/internal/audit-events",
                "/error", "/error",
                "/api/v1/admin/users", "/api/v1/admin/users");

        assertTrue(filter.shouldNotFilter(request));
        assertTrue(filter.shouldNotFilter(request));
        assertFalse(filter.shouldNotFilter(request));
    }
}
