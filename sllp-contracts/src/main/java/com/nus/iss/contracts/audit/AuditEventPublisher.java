package com.nus.iss.contracts.audit;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import java.util.function.Consumer;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/** Small dependency-free client used by services to submit redacted audit metadata. */
public final class AuditEventPublisher {
    private final String service;
    private final URI ingestUri;
    private final String secret;
    private final ObjectMapper objectMapper;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();
    private final Counter writeFailures;

    public AuditEventPublisher(String service, String ingestUrl, String secret, ObjectMapper objectMapper) {
        this.service = service;
        this.ingestUri = URI.create(ingestUrl);
        this.secret = secret;
        this.objectMapper = objectMapper;
        this.writeFailures = Counter.builder("audit_write_failures").tag("service", service).register(Metrics.globalRegistry);
    }

    public void publish(String category, String action, String outcome, String actorType, String actorId, String actorName,
            String actorRole, String targetType, String targetId, String requestId, String sourceIp, String userAgent,
            String method, String path, int status, Consumer<Throwable> failureHandler) {
        try {
            ObjectNode event = objectMapper.createObjectNode();
            event.put("id", UUID.randomUUID().toString());
            event.put("occurredAt", Instant.now().toString());
            put(event, "service", service, 80);
            put(event, "category", category, 48);
            put(event, "action", action, 120);
            put(event, "outcome", outcome, 24);
            put(event, "actorType", actorType == null ? "ANONYMOUS" : actorType, 32);
            put(event, "actorId", actorId, 255);
            put(event, "actorName", actorName, 255);
            put(event, "actorRole", actorRole, 64);
            put(event, "targetType", targetType, 80);
            put(event, "targetId", targetId, 255);
            if (requestId != null) {
                try {
                    UUID.fromString(requestId);
                    event.put("requestId", requestId);
                } catch (IllegalArgumentException ignored) {
                    // A malformed request ID must not prevent the request from completing.
                }
            }
            put(event, "sourceIp", sourceIp, 64);
            put(event, "userAgent", userAgent, 512);
            put(event, "httpMethod", method, 12);
            put(event, "httpPath", path, 512);
            event.put("httpStatus", status);
            event.set("metadata", objectMapper.createObjectNode());
            String body = objectMapper.writeValueAsString(event);
            String timestamp = Long.toString(Instant.now().getEpochSecond());
            HttpRequest request = HttpRequest.newBuilder(ingestUri).timeout(Duration.ofSeconds(2))
                    .header("Content-Type", "application/json").header("X-Sllp-Audit-Timestamp", timestamp)
                    .header("X-Sllp-Audit-Signature", sign(timestamp + "." + body))
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            client.sendAsync(request, HttpResponse.BodyHandlers.discarding()).whenComplete((response, error) -> {
                if (error != null) fail(error, failureHandler);
                else if (response.statusCode() >= 300) fail(new IllegalStateException("audit ingestion returned " + response.statusCode()), failureHandler);
            });
        } catch (Exception error) { fail(error, failureHandler); }
    }

    private String sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

    private void fail(Throwable error, Consumer<Throwable> failureHandler) { writeFailures.increment(); failureHandler.accept(error); }

    private static void put(ObjectNode target, String name, String value, int maxLength) {
        if (value != null && !value.isBlank()) {
            target.put(name, truncate(value, maxLength));
        }
    }

    private static String truncate(String value, int maxLength) {
        return value == null ? null : value.substring(0, Math.min(value.length(), maxLength));
    }
}
