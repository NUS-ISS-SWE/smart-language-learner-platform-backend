package com.nus.iss.controller;

import com.nus.iss.config.SllpProperties;
import com.nus.iss.adapter.OpenAiStatusClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.nus.iss.repository.GradingRepository;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AgentMetadataController {
    private final SllpProperties properties;
    private final OpenAiStatusClient openAiStatusClient;
    private final GradingRepository gradingRepository;

    @GetMapping("/.well-known/agent-card.json")
    public Map<String, Object> card() {
        return Map.of(
                "name", "SLLP Grading Agent",
                "url", properties.getPublicBaseUrl() + "/a2a",
                "protocolVersion", "1.0",
                "skills", List.of(Map.of("id", "composition-grading", "name", "Composition grading")),
                "sllp", Map.of(
                        "contractVersion", "2.0",
                        "inputSchemaUrl", properties.getPublicBaseUrl()
                                + "/.well-known/sllp-contracts/composition-grading-v2-input.json",
                        "outputSchemaUrl", properties.getPublicBaseUrl()
                                + "/.well-known/sllp-contracts/composition-grading-v2-output.json"));
    }

    @GetMapping("/.well-known/sllp-contracts/composition-grading-v2-input.json")
    public Map<String, Object> inputSchema() {
        return Map.of(
                "$schema", "https://json-schema.org/draft/2020-12/schema",
                "title", "SLLP composition-grading v2 input artifact",
                "type", "object",
                "required", List.of("schemaVersion", "workflowId", "attempt", "composition", "evidence", "rubricSnapshot"),
                "properties", Map.of(
                        "composition", Map.of("type", "object", "required", List.of("userId", "title", "content", "language")),
                        "evidence", Map.of("type", "object"),
                        "rubricSnapshot", Map.of("type", "object", "required", List.of("version", "criteria"))));
    }

    @GetMapping("/.well-known/sllp-contracts/composition-grading-v2-output.json")
    public Map<String, Object> outputSchema() {
        return Map.of(
                "$schema", "https://json-schema.org/draft/2020-12/schema",
                "title", "SLLP composition-grading v2 output artifact",
                "type", "object",
                "required", List.of("overallScore", "rubricScores", "rubricVersion", "criteria", "confidence", "usage"),
                "properties", Map.of(
                        "overallScore", Map.of("type", "number", "minimum", 0, "maximum", 100),
                        "rubricScores", Map.of("type", "object"),
                        "rubricVersion", Map.of("type", "string", "minLength", 1),
                        "criteria", Map.of("type", "array", "minItems", 1),
                        "confidence", Map.of("type", "number", "minimum", 0, "maximum", 1),
                        "usage", Map.of("type", "object")));
    }

    @GetMapping({"/health", "/ready"})
    public ResponseEntity<Map<String, String>> health() {
        boolean openAiOperational = openAiStatusClient.isOperational();
        boolean databaseOperational = databaseOperational();
        if (openAiOperational && databaseOperational) {
            log.debug("Grading agent readiness check passed");
            return ResponseEntity.ok(Map.of("status", "ready", "openai", "operational"));
        }

        log.debug("Grading agent readiness check failed openAiOperational={} databaseOperational={}", openAiOperational, databaseOperational);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("status", "unavailable", "openai", "unavailable"));
    }

    @GetMapping("/live")
    public Map<String, String> live() {
        return Map.of("status", "alive");
    }

    private boolean databaseOperational() {
        try {
            gradingRepository.count();
            return true;
        } catch (RuntimeException error) {
            log.warn("Grading audit database readiness check failed category={}", error.getClass().getSimpleName());
            return false;
        }
    }
}
