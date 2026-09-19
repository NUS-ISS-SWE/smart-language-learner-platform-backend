package com.nus.iss.config;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import tools.jackson.databind.ObjectMapper;
import com.nus.iss.contracts.audit.AuditEventPublisher;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class GatewayAuditPublisher {
    private final AuditEventPublisher publisher;
    public GatewayAuditPublisher(SllpProperties properties, ObjectMapper mapper,
            @org.springframework.beans.factory.annotation.Value("${sllp.audit.ingest-secret:local-development-audit-secret-change-me}") String secret) {
        publisher = new AuditEventPublisher("api-gateway", properties.getAdminService() + "/internal/audit-events", secret, mapper);
    }
    public void denied(ServerWebExchange exchange, String action, String actorId, String actorName, String actorRole, int status) {
        var request = exchange.getRequest();
        String ip = request.getRemoteAddress() == null ? null : request.getRemoteAddress().getAddress().getHostAddress();
        publisher.publish("AUTHORIZATION", action, "DENIED", actorId == null ? "ANONYMOUS" : "USER", actorId, actorName, actorRole,
                "HTTP_RESOURCE", request.getPath().value(), request.getHeaders().getFirst("X-Sllp-Request-Id"), ip,
                request.getHeaders().getFirst("User-Agent"), request.getMethod().name(), request.getPath().value(), status,
                error -> log.error("audit_write_failed action={}", action, error));
    }
}
