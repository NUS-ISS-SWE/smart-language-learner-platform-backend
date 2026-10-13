package com.nus.iss.service.impl;

import com.nus.iss.config.SllpProperties;
import com.nus.iss.dto.GradingRequestDTO;
import com.nus.iss.exception.PromptInjectionDetectedException;
import com.nus.iss.model.LakeraGuardDetection;
import com.nus.iss.repository.LakeraGuardDetectionRepository;
import com.nus.iss.service.SafeguardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class SafeguardServiceImpl implements SafeguardService {
    private final RestClient restClient;
    private final SllpProperties.Lakera properties;
    private final LakeraGuardDetectionRepository repository;
    private final ObjectMapper objectMapper;

    public SafeguardServiceImpl(@Qualifier("SafeguardRestClient") RestClient restClient,
                                SllpProperties sllpProperties,
                                LakeraGuardDetectionRepository repository,
                                ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.properties = sllpProperties.getLakera();
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void screenAndRecord(GradingRequestDTO request) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            log.warn("Lakera Guard screening skipped because no API key is configured requestId={} compositionId={}",
                    request.getRequestId(), request.getCompositionId());
            return;
        }

        String screenedText = screenedText(request);
        GuardResult result;
        try {
            log.info("Calling Lakera Guard requestId={} compositionId={} endpoint={}",
                    request.getRequestId(), request.getCompositionId(), properties.getEndpoint());
            String responseBody = restClient.post()
                    .uri(properties.getEndpoint())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody(request, screenedText))
                    .retrieve()
                    .body(String.class);
            log.info("Lakera Guard response requestId={} compositionId={} body={}",
                    request.getRequestId(), request.getCompositionId(), responseBody);
            result = parse(responseBody);
        } catch (Exception error) {
            log.warn("Lakera Guard screening failed open requestId={} compositionId={} category={}",
                    request.getRequestId(), request.getCompositionId(), error.getClass().getSimpleName());
            return;
        }

        if (!result.detected()) {
            log.info("Lakera Guard screening completed requestId={} compositionId={} detected=false action={}",
                    request.getRequestId(), request.getCompositionId(), result.action());
            return;
        }

        try {
            repository.saveAndFlush(LakeraGuardDetection.builder()
                    .id(UUID.randomUUID())
                    .requestId(request.getRequestId())
                    .contextId(request.getContextId())
                    .compositionId(request.getCompositionId())
                    .userId(request.getUserId())
                    .screenedText(screenedText)
                    .providerRequestUuid(result.requestUuid())
                    .action(result.action())
                    .providerFlagged(result.providerFlagged())
                    .detectorBreakdown(result.detectorBreakdown())
                    .providerResponse(result.rawResponse())
                    .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                    .build());
        } catch (Exception error) {
            throw new IllegalStateException("could not persist Lakera Guard detection", error);
        }

        log.warn("Lakera Guard blocked grading requestId={} compositionId={} action={}",
                request.getRequestId(), request.getCompositionId(), result.action());
        throw new PromptInjectionDetectedException();
    }

    private GuardResult parse(String responseBody) throws Exception {
        if (responseBody == null || responseBody.isBlank()) {
            throw new IllegalStateException("Lakera Guard returned an empty response");
        }
        JsonNode response = objectMapper.readTree(responseBody);
        JsonNode flagged = response.get("flagged");
        JsonNode breakdown = response.get("breakdown");
        if (flagged == null || !flagged.isBoolean() || breakdown == null || !breakdown.isArray()) {
            throw new IllegalStateException("Lakera Guard returned an invalid response");
        }
        boolean breakdownDetected = false;
        for (JsonNode detector : breakdown) {
            if (detector.path("detected").asBoolean(false)) {
                breakdownDetected = true;
                break;
            }
        }
        return new GuardResult(
                flagged.asBoolean(),
                flagged.asBoolean() || breakdownDetected,
                response.path("action").asText(null),
                response.path("metadata").path("request_uuid").asText(null),
                breakdown.toString(),
                responseBody);
    }

    private Map<String, Object> requestBody(GradingRequestDTO request, String screenedText) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("messages", List.of(Map.of("role", "user", "content", screenedText)));
        body.put("breakdown", true);
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("request_id", request.getRequestId());
        metadata.put("context_id", request.getContextId());
        if (request.getCompositionId() != null) metadata.put("composition_id", request.getCompositionId().toString());
        metadata.put("user_id", request.getUserId().toString());
        body.put("metadata", metadata);
        return body;
    }

    private static String screenedText(GradingRequestDTO request) {
        return "Title: %s\n\nComposition:\n%s".formatted(request.getTitle(), request.getContent());
    }

    private record GuardResult(boolean providerFlagged, boolean detected, String action,
                               String requestUuid, String detectorBreakdown, String rawResponse) {
    }
}
