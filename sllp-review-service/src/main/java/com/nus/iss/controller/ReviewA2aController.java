package com.nus.iss.controller;

import com.nus.iss.agent.ReviewAgentService;
import com.nus.iss.agent.ReviewModelResponse;
import com.nus.iss.agent.ReviewScoringPolicy;
import com.nus.iss.config.SllpProperties;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class ReviewA2aController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReviewA2aController.class);

    private final ReviewAgentService service; private final SllpProperties properties; private final ObjectMapper mapper;
    public ReviewA2aController(ReviewAgentService service, SllpProperties properties, ObjectMapper mapper) { this.service = service; this.properties = properties; this.mapper = mapper; }
    @GetMapping("/.well-known/agent-card.json") public Map<String,Object> card() { return Map.of("name", "SLLP Review Agent", "url", properties.getPublicBaseUrl() + "/a2a", "protocolVersion", "1.0", "skills", List.of(Map.of("id", "assessment-review", "name", "Assessment review")), "sllp", Map.of("contractVersion", "2.0")); }
    @GetMapping({"/health", "/ready"}) public Map<String,String> health() { return Map.of("status", "ready"); }
    @PostMapping("/a2a") public ResponseEntity<Map<String,Object>> invoke(@RequestHeader(value="Authorization", required=false) String auth, @RequestBody Map<String,Object> body) {
        if (!authorized(auth)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        JsonNode root = mapper.valueToTree(body); String id = root.path("id").isTextual() ? root.path("id").asText() : UUID.randomUUID().toString();
        try {
            if (!"2.0".equals(root.path("jsonrpc").asText()) || !"SendMessage".equals(root.path("method").asText())) {
                throw new IllegalArgumentException("unsupported A2A request");
            }
            JsonNode message = root.path("params").path("message");
            JsonNode data = jsonPart(message.path("parts"));
            JsonNode composition = data.path("composition");
            boolean autoApproved = properties.isReviewAutoApprove();
            ReviewAgentService.Reviewed reviewed = autoApproved
                    ? developmentAutoApproval()
                    : service.review(data.toString(), composition.path("userId").asLong(), composition.path("language").asText());
            // Canonicalise at the A2A boundary as well as in the model service. This keeps the
            // published score deterministic even if a future internal caller bypasses the service.
            ReviewModelResponse result = ReviewScoringPolicy.canonicalize(reviewed.response());
            Map<String, Object> artifact = Map.of(
                    "decision", result.decision(), "confidence", result.confidence(),
                    "qualityScore", result.qualityScore(),
                    "checks", result.checks(),
                    "findings", result.findings() == null ? List.of() : result.findings(),
                    "modelVersion", reviewed.usage().get("model"),
                    "promptVersion", autoApproved ? "review-auto-approve-local-v1" : "review-v2",
                    "usage", reviewed.usage());
            return ResponseEntity.ok(Map.of("jsonrpc", "2.0", "id", id, "result", Map.of("task", Map.of(
                    "id", UUID.randomUUID().toString(), "contextId", message.path("contextId").asText(),
                    "status", Map.of("state", "TASK_STATE_COMPLETED"), "artifacts", List.of(Map.of(
                    "artifactId", UUID.randomUUID().toString(), "parts", List.of(Map.of(
                    "mediaType", "application/json", "data", artifact))))))));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.ok(Map.of("jsonrpc", "2.0", "id", id,
                    "error", Map.of("code", -32602, "message", "invalid A2A review request")));
        } catch (Exception ex) {
            LOGGER.warn("A2A review execution failed requestId={}: {}", id, ex.toString());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "jsonrpc", "2.0", "id", id,
                    "error", Map.of("code", -32603, "message", "review execution failed")));
        }
    }
    private static ReviewAgentService.Reviewed developmentAutoApproval() {
        return new ReviewAgentService.Reviewed(
                new ReviewModelResponse("APPROVE", 1.0, 100,
                        List.of(
                                new ReviewModelResponse.ReviewCheck("GROUNDING", "PASS", "Not evaluated in local auto-approve mode."),
                                new ReviewModelResponse.ReviewCheck("RUBRIC_COVERAGE", "PASS", "Not evaluated in local auto-approve mode."),
                                new ReviewModelResponse.ReviewCheck("EVIDENCE_TRACEABILITY", "PASS", "Not evaluated in local auto-approve mode."),
                                new ReviewModelResponse.ReviewCheck("ACTIONABILITY", "PASS", "Not evaluated in local auto-approve mode.")),
                        List.of(new ReviewModelResponse.Finding(null,
                                "Development-only auto-approval; the Review model was not invoked."))),
                Map.of("provider", "placeholder", "model", "review-auto-approve-local", "promptTokens", 0L,
                        "cachedInputTokens", 0L, "completionTokens", 0L, "totalTokens", 0L,
                        "pricingVersion", "development-placeholder-v1", "inputPriceMicrosPerMillion", 0L,
                        "cachedInputPriceMicrosPerMillion", 0L, "outputPriceMicrosPerMillion", 0L));
    }
    private static JsonNode jsonPart(JsonNode parts) { for (JsonNode part: parts) if ("application/json".equals(part.path("mediaType").asText()) && part.path("data").isObject()) return part.path("data"); throw new IllegalArgumentException("missing JSON part"); }
    private boolean authorized(String auth) { String expected = properties.getA2aToken(); String actual = auth != null && auth.startsWith("Bearer ") ? auth.substring(7) : ""; return expected != null && !expected.isBlank() && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8)); }
}
