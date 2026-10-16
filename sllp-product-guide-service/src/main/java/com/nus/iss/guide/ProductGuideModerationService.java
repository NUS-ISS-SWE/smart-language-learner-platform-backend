package com.nus.iss.guide;

import com.nus.iss.contracts.audit.AuditEventPublisher;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.moderation.CategoryScores;
import org.springframework.ai.moderation.Moderation;
import org.springframework.ai.moderation.ModerationModel;
import org.springframework.ai.moderation.ModerationPrompt;
import org.springframework.ai.moderation.ModerationResponse;
import org.springframework.ai.moderation.ModerationResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

/** Product Guide input gate. Output remains streamed and is intentionally not moderated in this release. */
@Service
@Slf4j
public class ProductGuideModerationService {
    private static final double THRESHOLD = 0.80;
    private final ModerationModel moderationModel;
    private final MeterRegistry meterRegistry;
    private final AuditEventPublisher auditPublisher;

    public ProductGuideModerationService(ModerationModel moderationModel, MeterRegistry meterRegistry, ObjectMapper mapper,
            @Value("${sllp.audit.ingest-url:http://localhost:8086/internal/audit-events}") String auditUrl,
            @Value("${sllp.audit.ingest-secret:}") String auditSecret) {
        this.moderationModel = moderationModel;
        this.meterRegistry = meterRegistry;
        this.auditPublisher = new AuditEventPublisher("product-guide-service", auditUrl, auditSecret, mapper);
    }

    public void moderateInput(String message, String userId, String username, String requestId) {
        String digest = sha256(message);
        try {
            Moderation moderation = moderation(moderationModel.call(new ModerationPrompt(message)));
            boolean blocked = moderation.getResults().stream().anyMatch(result -> result.isFlagged() || exceedsThreshold(result));
            if (blocked) {
                metric("BLOCKED");
                audit("MODERATION_INPUT", "DENIED", userId, username, requestId, digest, moderation.getModel(), "BLOCKED");
                log.warn("Product Guide input blocked requestId={} userId={}", requestId, userId);
                throw new ProductGuideModerationException(ProductGuideModerationException.Reason.BLOCKED, null);
            }
            metric("ALLOWED");
            audit("MODERATION_INPUT", "SUCCESS", userId, username, requestId, digest, moderation.getModel(), "ALLOWED");
        } catch (ProductGuideModerationException error) {
            throw error;
        } catch (Exception error) {
            metric("UNAVAILABLE");
            audit("MODERATION_INPUT", "FAILURE", userId, username, requestId, digest, "", "UNAVAILABLE");
            log.warn("Product Guide moderation unavailable requestId={} category={}", requestId, error.getClass().getSimpleName());
            throw new ProductGuideModerationException(ProductGuideModerationException.Reason.UNAVAILABLE, error);
        }
    }

    public void recordGeneration(String outcome, String userId, String username, String requestId, long latencyMs) {
        meterRegistry.counter("sllp_ai_invocations_total", "service", "product-guide-service", "operation", "CHAT", "outcome", outcome).increment();
        audit("CHAT", "SUCCESS".equals(outcome) ? "SUCCESS" : "FAILURE", userId, username, requestId, "", "", outcome, latencyMs);
    }

    private void metric(String decision) {
        meterRegistry.counter("sllp_ai_moderation_checks_total", "service", "product-guide-service", "stage", "INPUT", "decision", decision).increment();
    }

    private void audit(String action, String outcome, String userId, String username, String requestId, String targetId, String model, String decision) {
        audit(action, outcome, userId, username, requestId, targetId, model, decision, null);
    }

    private void audit(String action, String outcome, String userId, String username, String requestId, String targetId, String model, String decision, Long latencyMs) {
        Map<String, String> metadata = new java.util.LinkedHashMap<>();
        metadata.put("operation", action); metadata.put("decision", decision); metadata.put("provider", "OPENAI");
        if (model != null && !model.isBlank()) metadata.put("model", model);
        if (latencyMs != null) metadata.put("latencyMs", Long.toString(latencyMs));
        auditPublisher.publish("AI_INVOCATION", action, outcome, "USER", userId, username, null,
                "CONTENT_DIGEST", targetId == null || targetId.isBlank() ? null : targetId, requestId,
                null, null, null, null, 0, metadata,
                error -> log.warn("AI audit write failed action={} category={}", action, error.getClass().getSimpleName()));
    }

    private static Moderation moderation(ModerationResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) throw new IllegalStateException("moderation response was empty");
        Moderation moderation = response.getResult().getOutput();
        if (moderation.getResults() == null || moderation.getResults().isEmpty()) throw new IllegalStateException("moderation results were empty");
        return moderation;
    }

    private static boolean exceedsThreshold(ModerationResult result) {
        CategoryScores scores = result.getCategoryScores();
        if (scores == null) throw new IllegalStateException("moderation category scores were absent");
        return List.of(scores.getSexual(), scores.getHate(), scores.getHarassment(), scores.getSelfHarm(),
                scores.getSexualMinors(), scores.getHateThreatening(), scores.getViolenceGraphic(),
                scores.getSelfHarmIntent(), scores.getSelfHarmInstructions(), scores.getHarassmentThreatening(),
                scores.getViolence()).stream().anyMatch(score -> !Double.isFinite(score) || score < 0 || score > 1 || score >= THRESHOLD);
    }

    private static String sha256(String value) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception error) { throw new IllegalStateException("SHA-256 is unavailable", error); }
    }
}
