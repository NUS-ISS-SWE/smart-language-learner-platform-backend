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

@Component @Slf4j
public class AuditHttpFilter extends OncePerRequestFilter {
    private final AuditEventPublisher publisher;
    public AuditHttpFilter(ObjectMapper mapper, @Value("${sllp.audit.ingest-url:http://localhost:8086/internal/audit-events}") String url, @Value("${sllp.audit.ingest-secret}") String secret) { publisher = new AuditEventPublisher("composition-service", url, secret, mapper); }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException { chain.doFilter(request, response); int status = response.getStatus(); String path = request.getRequestURI(); if (!"GET".equals(request.getMethod()) || path.contains("/admin/") || status == 401 || status == 403) publish(request, status, path); }
    private void publish(HttpServletRequest r, int status, String path) { String category = path.contains("/admin/") ? "ADMIN" : "DATA_CHANGE"; String outcome = status == 401 || status == 403 ? "DENIED" : status >= 400 ? "FAILURE" : "SUCCESS"; String action = ("HTTP_" + r.getMethod() + "_" + path).replaceAll("[^A-Za-z0-9]+", "_").toUpperCase(); publisher.publish(category, action, outcome, r.getHeader("X-Sllp-User-Id") == null ? "ANONYMOUS" : "USER", r.getHeader("X-Sllp-User-Id"), r.getHeader("X-Sllp-Username"), r.getHeader("X-Sllp-Role"), "HTTP_RESOURCE", path, r.getHeader("X-Sllp-Request-Id"), r.getRemoteAddr(), r.getHeader("User-Agent"), r.getMethod(), path, status, error -> log.error("audit_write_failed action={}", action, error)); }
    @Override protected boolean shouldNotFilter(HttpServletRequest request) { return request.getRequestURI().startsWith("/actuator") || request.getRequestURI().equals("/error"); }
}
