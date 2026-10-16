package com.nus.iss.controller;

import com.nus.iss.config.SllpProperties;
import com.nus.iss.contracts.a2a.SllpA2aContract;
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
        return SllpA2aContract.agentCard(
                "SLLP Grading Agent",
                "Grades a submitted composition against the immutable SLLP rubric and returns traceable criterion assessments.",
                properties.getAgentVersion(), properties.getPublicBaseUrl(),
                "composition-grading", "Composition grading",
                "Scores every immutable rubric criterion using the composition and verified evidence.",
                List.of("assessment", "grading", "rubric", "writing"),
                "/.well-known/sllp-contracts/composition-grading-v2-input.json",
                "/.well-known/sllp-contracts/composition-grading-v2-output.json");
    }

    @GetMapping("/.well-known/sllp-contracts/composition-grading-v2-input.json")
    public Map<String, Object> inputSchema() {
        Map<String, Object> properties = SllpA2aContract.commonRequestProperties();
        properties.put("evidence", SllpA2aContract.evidenceArtifactSchema());
        properties.put("rubricSnapshot", SllpA2aContract.rubricSnapshotSchema());
        return SllpA2aContract.objectSchema(
                "SLLP composition-grading v2 input artifact",
                List.of("schemaVersion", "workflowId", "attempt", "composition", "evidence", "rubricSnapshot"),
                properties);
    }

    @GetMapping("/.well-known/sllp-contracts/composition-grading-v2-output.json")
    public Map<String, Object> outputSchema() {
        return SllpA2aContract.objectSchema(
                "SLLP composition-grading v2 output artifact",
                List.of("overallScore", "rubricScores", "rubricVersion", "criteria", "confidence", "modelVersion", "promptVersion", "usage"),
                Map.ofEntries(
                        Map.entry("overallScore", Map.of("type", "number", "minimum", 0, "maximum", 100)),
                        Map.entry("rubricScores", Map.of("type", "object", "additionalProperties", Map.of("type", "number", "minimum", 0, "maximum", 100))),
                        Map.entry("rubricVersion", Map.of("type", "string", "minLength", 1)),
                        Map.entry("criteria", gradingCriteriaSchema()),
                        Map.entry("confidence", Map.of("type", "number", "minimum", 0, "maximum", 1)),
                        Map.entry("modelVersion", Map.of("type", "string", "minLength", 1)),
                        Map.entry("promptVersion", Map.of("type", "string", "minLength", 1)),
                        Map.entry("usage", SllpA2aContract.usageSchema())));
    }

    private static Map<String, Object> gradingCriteriaSchema() {
        return Map.of("type", "array", "minItems", 1, "items", Map.of("type", "object", "required", List.of("criterion", "bandScore", "score", "rationale", "evidence"), "properties", Map.of(
                "criterion", Map.of("type", "string", "minLength", 1), "bandScore", Map.of("type", "integer", "minimum", 1, "maximum", 5),
                "score", Map.of("type", "number", "minimum", 0, "maximum", 100), "rationale", Map.of("type", "string", "minLength", 1),
                "evidence", Map.of("type", "array", "minItems", 1, "items", Map.of("type", "object", "required", List.of("excerpt"), "properties", Map.of(
                        "excerpt", Map.of("type", "string", "minLength", 1), "location", Map.of("type", "string", "minLength", 1)))))));
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
