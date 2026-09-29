package com.nus.iss.controller;

import com.nus.iss.config.SllpProperties;
import com.nus.iss.service.OpenAiStatusClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AgentMetadataController {
    private final SllpProperties properties;
    private final OpenAiStatusClient openAiStatusClient;

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
    public ResponseEntity<Map<String, String>> health() {
        if (openAiStatusClient.isOperational()) {
            return ResponseEntity.ok(Map.of("status", "ready", "openai", "operational"));
        }

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("status", "unavailable", "openai", "unavailable"));
    }

    @GetMapping("/live")
    public Map<String, String> live() {
        return Map.of("status", "alive");
    }
}
