package com.nus.iss.service.impl;

import com.nus.iss.config.ModerationConfig.ModerationPolicy;
import com.nus.iss.dto.GradingRequestDTO;
import com.nus.iss.model.CompositionModerationFlag;
import com.nus.iss.repository.CompositionModerationFlagRepository;
import com.nus.iss.service.ModerationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.ai.moderation.CategoryScores;
import org.springframework.ai.moderation.Moderation;
import org.springframework.ai.moderation.ModerationModel;
import org.springframework.ai.moderation.ModerationPrompt;
import org.springframework.ai.moderation.ModerationResponse;
import org.springframework.ai.moderation.ModerationResult;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class ModerationServiceImpl implements ModerationService {
    private final ModerationModel moderationModel;
    private final ModerationPolicy moderationPolicy;
    private final CompositionModerationFlagRepository repository;
    private final ObjectMapper objectMapper;

    public ModerationServiceImpl(ModerationModel moderationModel,
                                 @Qualifier("lenientModerationPolicy") ModerationPolicy moderationPolicy,
                                 CompositionModerationFlagRepository repository,
                                 ObjectMapper objectMapper) {
        this.moderationModel = moderationModel;
        this.moderationPolicy = moderationPolicy;
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void moderateAndRecord(GradingRequestDTO request) {
        try {
            String moderatedText = moderatedText(request);
            ModerationResponse response = moderationModel.call(new ModerationPrompt(moderatedText));
            Moderation moderation = moderation(response);
            Map<String, Double> scores = scores(moderation.getResults());
            Map<String, Double> matchedScores = new LinkedHashMap<>();
            scores.forEach((category, score) -> {
                if (moderationPolicy.isFlagged(score)) {
                    matchedScores.put(category, score);
                }
            });
            if (matchedScores.isEmpty()) {
                log.info("Composition moderation completed requestId={} compositionId={} flagged=false",
                        request.getRequestId(), request.getCompositionId());
                return;
            }

            boolean providerFlagged = moderation.getResults().stream().anyMatch(ModerationResult::isFlagged);
            repository.saveAndFlush(CompositionModerationFlag.builder()
                    .id(UUID.randomUUID())
                    .requestId(request.getRequestId())
                    .contextId(request.getContextId())
                    .compositionId(request.getCompositionId())
                    .userId(request.getUserId())
                    .moderatedText(moderatedText)
                    .providerResponseId(moderation.getId())
                    .model(moderation.getModel())
                    .providerFlagged(providerFlagged)
                    .matchedCategoryScores(objectMapper.writeValueAsString(matchedScores))
                    .threshold(moderationPolicy.threshold())
                    .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                    .build());
            log.warn("Composition moderation flagged requestId={} compositionId={} categories={}",
                    request.getRequestId(), request.getCompositionId(), matchedScores.keySet());
        } catch (Exception error) {
            log.warn("Composition moderation failed open requestId={} compositionId={} category={}",
                    request == null ? null : request.getRequestId(),
                    request == null ? null : request.getCompositionId(),
                    error.getClass().getSimpleName());
        }
    }

    private static String moderatedText(GradingRequestDTO request) {
        return "Title: %s\n\nComposition:\n%s".formatted(request.getTitle(), request.getContent());
    }

    private static Moderation moderation(ModerationResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            throw new IllegalStateException("OpenAI moderation returned no result");
        }
        Moderation moderation = response.getResult().getOutput();
        if (moderation.getResults() == null || moderation.getResults().isEmpty()) {
            throw new IllegalStateException("OpenAI moderation returned no category results");
        }
        return moderation;
    }

    private static Map<String, Double> scores(List<ModerationResult> results) {
        Map<String, Double> scores = new LinkedHashMap<>();
        for (ModerationResult result : results) {
            if (result == null || result.getCategoryScores() == null) {
                throw new IllegalStateException("OpenAI moderation returned incomplete category scores");
            }
            CategoryScores categoryScores = result.getCategoryScores();
            merge(scores, "sexual", categoryScores.getSexual());
            merge(scores, "hate", categoryScores.getHate());
            merge(scores, "harassment", categoryScores.getHarassment());
            merge(scores, "self-harm", categoryScores.getSelfHarm());
            merge(scores, "sexual/minors", categoryScores.getSexualMinors());
            merge(scores, "hate/threatening", categoryScores.getHateThreatening());
            merge(scores, "violence/graphic", categoryScores.getViolenceGraphic());
            merge(scores, "self-harm/intent", categoryScores.getSelfHarmIntent());
            merge(scores, "self-harm/instructions", categoryScores.getSelfHarmInstructions());
            merge(scores, "harassment/threatening", categoryScores.getHarassmentThreatening());
            merge(scores, "violence", categoryScores.getViolence());
        }
        return scores;
    }

    private static void merge(Map<String, Double> scores, String category, double score) {
        if (!Double.isFinite(score) || score < 0 || score > 1) {
            throw new IllegalStateException("OpenAI moderation returned an invalid category score");
        }
        scores.merge(category, score, Math::max);
    }
}
