package com.nus.iss.controller;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.nus.iss.agent.EvidenceAgentRequest;
import com.nus.iss.config.SllpProperties;
import com.nus.iss.dto.AiUsage;
import com.nus.iss.dto.CriterionEvidence;
import com.nus.iss.dto.EvidenceCollectionResult;
import com.nus.iss.dto.EvidenceItem;
import com.nus.iss.dto.RubricCriterionDTO;
import com.nus.iss.enums.Language;
import com.nus.iss.service.impl.EvidenceCollectionServiceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** A2A v2 adapter; the evidence domain service remains transport-agnostic. */
@RestController
public class EvidenceA2aController {
    private final EvidenceCollectionServiceImpl service;
    private final SllpProperties properties;
    private final ObjectMapper objectMapper;

    public EvidenceA2aController(EvidenceCollectionServiceImpl service, SllpProperties properties, ObjectMapper objectMapper) {
        this.service = service;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/.well-known/agent-card.json")
    public Map<String, Object> card() {
        return Map.of("name", "SLLP Evidence Agent", "url", properties.getPublicBaseUrl() + "/a2a", "protocolVersion", "1.0",
                "skills", List.of(Map.of("id", "evidence-collection", "name", "Evidence collection")),
                "sllp", Map.of(
                        "contractVersion", "2.0",
                        "inputSchemaUrl", properties.getPublicBaseUrl() + "/.well-known/sllp-contracts/evidence-collection-v2-input.json",
                        "outputSchemaUrl", properties.getPublicBaseUrl() + "/.well-known/sllp-contracts/evidence-collection-v2-output.json"));
    }

    @GetMapping("/.well-known/sllp-contracts/evidence-collection-v2-input.json")
    public Map<String, Object> inputSchema() {
        return Map.of(
                "$schema", "https://json-schema.org/draft/2020-12/schema",
                "title", "Evidence collection input",
                "type", "object",
                "required", List.of("schemaVersion", "workflowId", "attempt", "composition"),
                "properties", Map.of("composition", Map.of(
                        "type", "object",
                        "required", List.of("compositionId", "userId", "title", "content", "language"))));
    }

    @GetMapping("/.well-known/sllp-contracts/evidence-collection-v2-output.json")
    public Map<String, Object> outputSchema() {
        return Map.of(
                "$schema", "https://json-schema.org/draft/2020-12/schema",
                "title", "Evidence collection output",
                "type", "object",
                "required", List.of("criteria", "rubricVersion", "rubricSnapshot", "usage"),
                "properties", Map.of(
                        "criteria", Map.of("type", "array", "minItems", 1),
                        "rubricVersion", Map.of("type", "string", "minLength", 1),
                        "rubricSnapshot", Map.of("type", "object", "required", List.of("version", "criteria")),
                        "usage", Map.of("type", "object")));
    }

    @GetMapping({"/health", "/ready"})
    public Map<String, String> health() {
        return Map.of("status", "ready");
    }

    @PostMapping("/a2a")
    public ResponseEntity<Map<String, Object>> invoke(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        if (!authorized(authorization)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        JsonNode root = objectMapper.valueToTree(body);
        String requestId = root.path("id").isTextual() ? root.path("id").asText() : UUID.randomUUID().toString();
        try {
            if (!"2.0".equals(root.path("jsonrpc").asText()) || !"SendMessage".equals(root.path("method").asText())) {
                throw new IllegalArgumentException("unsupported A2A request");
            }
            JsonNode message = root.path("params").path("message");
            String contextId = message.path("contextId").asText();
            JsonNode data = jsonPart(message.path("parts"));
            EvidenceAgentRequest request = new EvidenceAgentRequest(
                    data.path("composition").path("compositionId").asLong(),
                    data.path("composition").path("userId").asLong(),
                    data.path("composition").path("title").asText(),
                    data.path("composition").path("content").asText(),
                    Language.valueOf(data.path("composition").path("language").asText()));
            if (request.compositionId() <= 0 || request.userId() <= 0 || request.content().isBlank()) throw new IllegalArgumentException("invalid composition");
            EvidenceCollectionResult result = service.collect(request, requestId);
            return ResponseEntity.ok(completed(requestId, contextId, artifact(result)));
        } catch (Exception error) {
            return ResponseEntity.ok(Map.of("jsonrpc", "2.0", "id", requestId, "error", Map.of("code", -32602, "message", "invalid A2A evidence request")));
        }
    }

    private static JsonNode jsonPart(JsonNode parts) {
        for (JsonNode part : parts) if ("application/json".equals(part.path("mediaType").asText()) && part.path("data").isObject()) return part.path("data");
        throw new IllegalArgumentException("missing JSON part");
    }

    private Map<String, Object> artifact(EvidenceCollectionResult result) {
        List<Map<String, Object>> criteria = result.criteria().stream().map(this::criterion).toList();
        Map<String, Object> artifact = new LinkedHashMap<>();
        artifact.put("plan", result.plan());
        artifact.put("criteria", criteria);
        Map<String, Object> rubricSnapshot = rubricSnapshot(result.rubric());
        artifact.put("rubricVersion", rubricSnapshot.get("version"));
        artifact.put("rubricSnapshot", rubricSnapshot);
        artifact.put("modelVersion", result.usage().model());
        artifact.put("promptVersion", "evidence-v2");
        artifact.put("usage", usage(result.usage()));
        return artifact;
    }

    /**
     * This is the immutable assessment framework for one workflow attempt.  Downstream peers
     * must use it rather than querying Admin again, so an administrator edit cannot make one
     * assessment internally inconsistent.
     */
    private static Map<String, Object> rubricSnapshot(List<RubricCriterionDTO> rubric) {
        if (rubric == null || rubric.isEmpty()) throw new IllegalStateException("evidence returned no rubric");
        List<Map<String, Object>> criteria = rubric.stream()
                .sorted(Comparator.comparing(RubricCriterionDTO::criterionName))
                .map(item -> Map.<String, Object>of(
                        "name", item.criterionName(),
                        "weight", item.weightPercentage(),
                        "guidance", item.criterionDescription()))
                .toList();
        BigDecimal total = rubric.stream().map(RubricCriterionDTO::weightPercentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(BigDecimal.valueOf(100)) != 0) {
            throw new IllegalStateException("Admin rubric weights must total 100");
        }
        String canonical = criteria.stream()
                .map(item -> item.get("name") + "|" + item.get("weight") + "|" + item.get("guidance"))
                .collect(java.util.stream.Collectors.joining("\\n"));
        String version = "admin-rubric-" + sha256(canonical).substring(0, 12);
        return Map.of("version", version, "criteria", criteria);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte item : digest) result.append(String.format("%02x", item));
            return result.toString();
        } catch (java.security.NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
    }

    private Map<String, Object> criterion(CriterionEvidence criterion) {
        return Map.of("criterion", criterion.criterion(), "evidence", criterion.evidence().stream().map(this::evidence).toList());
    }

    private Map<String, Object> evidence(EvidenceItem item) {
        return Map.of("excerpt", item.excerpt(), "polarity", item.polarity().name(), "note", item.note(), "verifiedOnFirstPass", item.verifiedOnFirstPass());
    }

    private static Map<String, Object> usage(AiUsage usage) {
        return Map.of("provider", usage.provider(), "model", usage.model(), "promptTokens", usage.promptTokens(),
                "cachedInputTokens", usage.cachedInputTokens(), "completionTokens", usage.completionTokens(), "totalTokens", usage.totalTokens(),
                "pricingVersion", usage.pricingVersion(), "inputPriceMicrosPerMillion", usage.inputPriceMicrosPerMillion(),
                "cachedInputPriceMicrosPerMillion", usage.cachedInputPriceMicrosPerMillion(), "outputPriceMicrosPerMillion", usage.outputPriceMicrosPerMillion());
    }

    private static Map<String, Object> completed(String requestId, String contextId, Map<String, Object> artifact) {
        return Map.of("jsonrpc", "2.0", "id", requestId, "result", Map.of("task", Map.of("id", UUID.randomUUID().toString(),
                "contextId", contextId, "status", Map.of("state", "TASK_STATE_COMPLETED"),
                "artifacts", List.of(Map.of("artifactId", UUID.randomUUID().toString(), "parts", List.of(Map.of("mediaType", "application/json", "data", artifact)))))));
    }

    private boolean authorized(String authorization) {
        String expected = properties.getA2aToken();
        String received = authorization != null && authorization.startsWith("Bearer ") ? authorization.substring(7) : "";
        return expected != null && !expected.isBlank() && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), received.getBytes(StandardCharsets.UTF_8));
    }
}
