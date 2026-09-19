package com.nus.iss.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import tools.jackson.databind.ObjectMapper;
import com.nus.iss.contracts.audit.AuditEventPublisher;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/** Captures security, state-change, and privileged-read metadata; bodies are never inspected. */
@Component
@Slf4j
public class AuditHttpFilter extends OncePerRequestFilter {
    private final AuditEventPublisher publisher;
    public AuditHttpFilter(ObjectMapper mapper, @Value("${sllp.audit.ingest-url:http://localhost:8086/internal/audit-events}") String url,
            @Value("${sllp.audit.ingest-secret}") String secret) { publisher = new AuditEventPublisher("user-service", url, secret, mapper); }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        chain.doFilter(request, response);
        int status = response.getStatus(); String path = request.getRequestURI(); boolean mutation = !"GET".equals(request.getMethod()) && !"HEAD".equals(request.getMethod());
        if (mutation || path.contains("/admin/") || status == 401 || status == 403) publish(request, status, path);
    }
    private void publish(HttpServletRequest request, int status, String path) {
        String category = path.contains("/auth/") ? "AUTHENTICATION" : path.contains("/admin/") ? "ADMIN" : "DATA_CHANGE";
        String outcome = status == 401 || status == 403 ? "DENIED" : status >= 400 ? "FAILURE" : "SUCCESS";
        String action = ("HTTP_" + request.getMethod() + "_" + path).replaceAll("[^A-Za-z0-9]+", "_").toUpperCase();
        publisher.publish(category, action, outcome, request.getHeader("X-Sllp-User-Id") == null ? "ANONYMOUS" : "USER", request.getHeader("X-Sllp-User-Id"), request.getHeader("X-Sllp-Username"), request.getHeader("X-Sllp-Role"), "HTTP_RESOURCE", path, request.getHeader("X-Sllp-Request-Id"), request.getRemoteAddr(), request.getHeader("User-Agent"), request.getMethod(), path, status, error -> log.error("audit_write_failed action={}", action, error));
    }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) { return request.getRequestURI().startsWith("/actuator") || request.getRequestURI().equals("/error"); }
}
