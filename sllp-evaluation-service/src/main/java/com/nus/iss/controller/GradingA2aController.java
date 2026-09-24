package com.nus.iss.controller;

import com.nus.iss.agent.GradingAgentRequest;
import com.nus.iss.agent.GradingAgentService;
import com.nus.iss.config.SllpProperties;
import com.nus.iss.dto.A2aGradingResult;
import com.nus.iss.dto.AiUsage;
import com.nus.iss.enums.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;

/** Transport adapter for the version 2 SLLP composition-grading A2A contract. */
@RestController
public class GradingA2aController {
    private static final Logger LOGGER = LoggerFactory.getLogger(GradingA2aController.class);
    private static final String JSON_RPC_VERSION = "2.0";
    private static final String SEND_MESSAGE = "SendMessage";

    private final GradingAgentService service;
    private final SllpProperties properties;
    private final ObjectMapper objectMapper;

    public GradingA2aController(GradingAgentService service, SllpProperties properties, ObjectMapper objectMapper) {
        this.service = service;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/.well-known/agent-card.json")
    public Map<String, Object> card() {
        return Map.of(
                "name", "SLLP Grading Agent",
                "url", properties.getPublicBaseUrl() + "/a2a",
                "protocolVersion", "1.0",
                "skills", List.of(Map.of("id", "composition-grading", "name", "Composition grading")),
                "sllp", Map.of("contractVersion", "2.0"));
    }

    @GetMapping({"/health", "/ready"})
    public Map<String, String> health() {
        return Map.of("status", "ready");
    }

    @PostMapping("/a2a")
    public ResponseEntity<Map<String, Object>> invoke(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        if (!authorized(authorization)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        JsonNode root = objectMapper.valueToTree(body);
        String requestId = requestId(root);
        try {
            A2aGradingRequest request = parseRequest(root);
            A2aGradingResult result = service.gradeStructured(request.gradingRequest());
            return ResponseEntity.ok(completed(requestId, request.contextId(), result, request.gradingRequest().rubricSnapshot()));
        } catch (IllegalArgumentException error) {
            return ResponseEntity.ok(jsonRpcError(requestId, -32602, "invalid A2A grading request"));
        } catch (Exception error) {
            // Preserve detail in peer logs only; the orchestrator treats this as retriable.
            LOGGER.warn("A2A grading execution failed requestId={}: {}", requestId, error.toString());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(jsonRpcError(requestId, -32603, "grading execution failed"));
        }
    }

    private static A2aGradingRequest parseRequest(JsonNode root) {
        if (!JSON_RPC_VERSION.equals(root.path("jsonrpc").asText()) || !SEND_MESSAGE.equals(root.path("method").asText())) {
            throw new IllegalArgumentException("unsupported A2A request");
        }
        JsonNode message = root.path("params").path("message");
        JsonNode data = jsonPart(message.path("parts"));
        JsonNode composition = data.path("composition");
        List<String> evidence = excerpts(data.path("evidence").path("criteria"));
        GradingAgentRequest.RubricSnapshot snapshot = rubricSnapshot(data.path("evidence").path("rubricSnapshot"));
        GradingAgentRequest request = new GradingAgentRequest(
                composition.path("userId").asLong(),
                composition.path("title").asText(),
                composition.path("content").asText(),
                Language.valueOf(composition.path("language").asText()),
                snapshot,
                evidence);
        return new A2aGradingRequest(message.path("contextId").asText(), request);
    }

    private static String requestId(JsonNode root) {
        return root.path("id").isTextual() ? root.path("id").asText() : UUID.randomUUID().toString();
    }

    private static JsonNode jsonPart(JsonNode parts) {
        for (JsonNode part : parts) {
            if ("application/json".equals(part.path("mediaType").asText()) && part.path("data").isObject()) {
                return part.path("data");
            }
        }
        throw new IllegalArgumentException("missing JSON part");
    }

    private static List<String> excerpts(JsonNode criteria) {
        return StreamSupport.stream(criteria.spliterator(), false)
                .flatMap(criterion -> StreamSupport.stream(criterion.path("evidence").spliterator(), false))
                .map(evidence -> evidence.path("excerpt").asText())
                .toList();
    }

    private static GradingAgentRequest.RubricSnapshot rubricSnapshot(JsonNode node) {
        if (!node.isObject() || node.path("version").asText().isBlank() || !node.path("criteria").isArray()) {
            throw new IllegalArgumentException("missing rubric snapshot");
        }
        List<GradingAgentRequest.RubricSnapshot.Criterion> criteria = StreamSupport.stream(node.path("criteria").spliterator(), false)
                .map(item -> new GradingAgentRequest.RubricSnapshot.Criterion(
                        item.path("name").asText(),
                        new BigDecimal(item.path("weight").asText()),
                        item.path("guidance").asText()))
                .toList();
        return new GradingAgentRequest.RubricSnapshot(node.path("version").asText(), criteria);
    }

    private static Map<String, Object> completed(
            String requestId,
            String contextId,
            A2aGradingResult result,
            GradingAgentRequest.RubricSnapshot snapshot) {
        Map<String, Object> artifact = Map.of(
                "overallScore", result.overallScore(),
                "rubricScores", result.rubricScores(),
                "criteria", result.criteria(),
                "confidence", result.confidence(),
                "rubricVersion", snapshot.version(),
                "rubricSnapshot", snapshot,
                "modelVersion", result.modelVersion(),
                "promptVersion", result.promptVersion(),
                "usage", usage(result.usage()));
        Map<String, Object> task = Map.of(
                "id", UUID.randomUUID().toString(),
                "contextId", contextId,
                "status", Map.of("state", "TASK_STATE_COMPLETED"),
                "artifacts", List.of(Map.of(
                        "artifactId", UUID.randomUUID().toString(),
                        "parts", List.of(Map.of("mediaType", "application/json", "data", artifact)))));
        return Map.of("jsonrpc", JSON_RPC_VERSION, "id", requestId, "result", Map.of("task", task));
    }

    private static Map<String, Object> usage(AiUsage usage) {
        return Map.of(
                "provider", usage.provider(),
                "model", usage.model(),
                "promptTokens", usage.promptTokens(),
                "cachedInputTokens", usage.cachedInputTokens(),
                "completionTokens", usage.completionTokens(),
                "totalTokens", usage.totalTokens(),
                "pricingVersion", usage.pricingVersion(),
                "inputPriceMicrosPerMillion", usage.inputPriceMicrosPerMillion(),
                "cachedInputPriceMicrosPerMillion", usage.cachedInputPriceMicrosPerMillion(),
                "outputPriceMicrosPerMillion", usage.outputPriceMicrosPerMillion());
    }

    private static Map<String, Object> jsonRpcError(String requestId, int code, String message) {
        return Map.of("jsonrpc", JSON_RPC_VERSION, "id", requestId, "error", Map.of("code", code, "message", message));
    }

    private boolean authorized(String authorization) {
        String expected = properties.getA2aToken();
        String received = authorization != null && authorization.startsWith("Bearer ") ? authorization.substring(7) : "";
        return expected != null && !expected.isBlank()
                && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), received.getBytes(StandardCharsets.UTF_8));
    }

    private record A2aGradingRequest(String contextId, GradingAgentRequest gradingRequest) {
    }
}
