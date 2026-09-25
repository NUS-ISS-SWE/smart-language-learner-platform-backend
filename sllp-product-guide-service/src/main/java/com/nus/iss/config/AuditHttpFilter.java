package com.nus.iss.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.nus.iss.contracts.audit.AuditEventPublisher;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

/** Records Product Guide access metadata without retaining chat prompts or responses. */
@Component
@Slf4j
public class AuditHttpFilter extends OncePerRequestFilter {
    private final AuditEventPublisher publisher;

    public AuditHttpFilter(ObjectMapper mapper,
            @Value("${sllp.audit.ingest-url:http://localhost:8086/internal/audit-events}") String url,
            @Value("${sllp.audit.ingest-secret:}") String secret) {
        publisher = new AuditEventPublisher("product-guide-service", url, secret, mapper);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        chain.doFilter(request, response);
        String path = request.getRequestURI();
        if (path.startsWith("/api/v1/product-guide/")) publish(request, response.getStatus(), path);
    }

    private void publish(HttpServletRequest request, int status, String path) {
        String outcome = status >= 400 ? "FAILURE" : "SUCCESS";
        String action = ("HTTP_" + request.getMethod() + "_" + path).replaceAll("[^A-Za-z0-9]+", "_").toUpperCase();
        publisher.publish("DATA_CHANGE", action, outcome,
                request.getHeader("X-Sllp-User-Id") == null ? "ANONYMOUS" : "USER",
                request.getHeader("X-Sllp-User-Id"), request.getHeader("X-Sllp-Username"), request.getHeader("X-Sllp-Role"),
                "HTTP_RESOURCE", path, request.getHeader("X-Sllp-Request-Id"), request.getRemoteAddr(), request.getHeader("User-Agent"),
                request.getMethod(), path, status, error -> log.error("audit_write_failed action={}", action, error));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator") || path.equals("/error");
    }
}
