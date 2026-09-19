package com.nus.iss.audit;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Records privileged reads centrally without recursively auditing the internal ingestion endpoint. */
@Component
public class AdminAuditHttpFilter extends OncePerRequestFilter {
    private final AdminAuditRecorder recorder;
    public AdminAuditHttpFilter(AdminAuditRecorder recorder) { this.recorder = recorder; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        chain.doFilter(request, response);
        String path = request.getRequestURI();
        if (path.startsWith("/api/v1/admin/") && response.getStatus() < 400) {
            recorder.record("ADMIN_RESOURCE_VIEWED", request.getHeader("X-Sllp-Username"), request.getHeader("X-Sllp-User-Id"), "HTTP_RESOURCE", path,
                    request.getHeader("X-Sllp-Request-Id"), request.getMethod(), path);
        }
    }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) { return request.getRequestURI().startsWith("/internal/") || request.getRequestURI().equals("/error"); }
}
