package com.nus.iss.service.impl;

import com.nus.iss.config.ModerationConfig.ModerationPolicy;
import com.nus.iss.dto.AiGradingOutputDTO;
import com.nus.iss.dto.GradingRequestDTO;
import com.nus.iss.exception.ModerationBlockedException;
import com.nus.iss.exception.ModerationUnavailableException;
import com.nus.iss.model.CompositionModerationFlag;
import com.nus.iss.repository.CompositionModerationFlagRepository;
import com.nus.iss.service.ModerationService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.moderation.CategoryScores;
import org.springframework.ai.moderation.Moderation;
import org.springframework.ai.moderation.ModerationModel;
import org.springframework.ai.moderation.ModerationPrompt;
import org.springframework.ai.moderation.ModerationResponse;
import org.springframework.ai.moderation.ModerationResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Enforces a fail-closed moderation boundary before generation and learner feedback release. */
@Service
@Slf4j
public class ModerationServiceImpl implements ModerationService {
    private static final String COMPOSITION_INPUT = "COMPOSITION_INPUT";
    private static final String LEARNER_FEEDBACK = "LEARNER_FEEDBACK";

    private final ModerationModel moderationModel;
    private final ModerationPolicy moderationPolicy;
    private final CompositionModerationFlagRepository repository;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final ModerationAuditPublisher auditPublisher;

    public ModerationServiceImpl(ModerationModel moderationModel,
                                 @Qualifier("lenientModerationPolicy") ModerationPolicy moderationPolicy,
                                 CompositionModerationFlagRepository repository,
                                 ObjectMapper objectMapper,
                                 MeterRegistry meterRegistry,
                                 ModerationAuditPublisher auditPublisher) {
        this.moderationModel = moderationModel;
        this.moderationPolicy = moderationPolicy;
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.auditPublisher = auditPublisher;
    }

    @Override
    public void moderateComposition(GradingRequestDTO request) {
        moderate(request, COMPOSITION_INPUT, "Title: %s\n\nComposition:\n%s".formatted(request.getTitle(), request.getContent()));
    }

    @Override
    public void moderateFeedback(GradingRequestDTO request, AiGradingOutputDTO output) {
        try {
            moderate(request, LEARNER_FEEDBACK, objectMapper.writeValueAsString(output));
        } catch (ModerationBlockedException | ModerationUnavailableException error) {
            throw error;
        } catch (Exception error) {
            unavailable(request, LEARNER_FEEDBACK, error);
        }
    }

    private void moderate(GradingRequestDTO request, String stage, String text) {
        try {
            Moderation moderation = moderation(moderationModel.call(new ModerationPrompt(text)));
            Map<String, Double> scores = scores(moderation.getResults());
            Map<String, Double> matchedScores = new LinkedHashMap<>();
            scores.forEach((category, score) -> {
                if (moderationPolicy.isFlagged(score)) matchedScores.put(category, score);
            });
            boolean providerFlagged = moderation.getResults().stream().anyMatch(ModerationResult::isFlagged);
            if (!providerFlagged && matchedScores.isEmpty()) {
                metric(stage, "ALLOWED");
                auditPublisher.publish(request, stage, "SUCCESS", moderation.getModel(), moderationPolicy.threshold());
                log.info("Moderation allowed requestId={} compositionId={} stage={}", request.getRequestId(), request.getCompositionId(), stage);
                return;
            }
            saveBlocked(request, stage, text, moderation, providerFlagged, matchedScores);
            metric(stage, "BLOCKED");
            auditPublisher.publish(request, stage, "DENIED", moderation.getModel(), moderationPolicy.threshold());
            log.warn("Moderation blocked requestId={} compositionId={} stage={} categories={}", request.getRequestId(), request.getCompositionId(), stage, matchedScores.keySet());
            throw new ModerationBlockedException(request.getRequestId());
        } catch (ModerationBlockedException error) {
            throw error;
        } catch (Exception error) {
            unavailable(request, stage, error);
        }
    }

    private void saveBlocked(GradingRequestDTO request, String stage, String text, Moderation moderation,
                             boolean providerFlagged, Map<String, Double> matchedScores) throws Exception {
        repository.saveAndFlush(CompositionModerationFlag.builder()
                .id(UUID.randomUUID()).requestId(request.getRequestId()).contextId(request.getContextId())
                .compositionId(request.getCompositionId()).userId(request.getUserId())
                .contentSha256(sha256(text)).contentLength(text.length()).stage(stage).decision("BLOCKED")
                .providerResponseId(moderation.getId()).model(moderation.getModel()).providerFlagged(providerFlagged)
                .matchedCategoryScores(objectMapper.writeValueAsString(matchedScores)).threshold(moderationPolicy.threshold())
                .createdAt(LocalDateTime.now(ZoneOffset.UTC)).build());
    }

    private void unavailable(GradingRequestDTO request, String stage, Exception error) {
        metric(stage, "UNAVAILABLE");
        if (request != null) auditPublisher.publish(request, stage, "FAILURE", null, moderationPolicy.threshold());
        log.warn("Moderation unavailable requestId={} compositionId={} stage={} category={}", request == null ? null : request.getRequestId(), request == null ? null : request.getCompositionId(), stage, error.getClass().getSimpleName());
        throw new ModerationUnavailableException(request == null ? null : request.getRequestId(), error);
    }

    private void metric(String stage, String decision) {
        meterRegistry.counter("sllp_ai_moderation_checks_total", "service", "grading-agent", "stage", stage, "decision", decision).increment();
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (Exception error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
    }

    private static Moderation moderation(ModerationResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) throw new IllegalStateException("OpenAI moderation returned no result");
        Moderation moderation = response.getResult().getOutput();
        if (moderation.getResults() == null || moderation.getResults().isEmpty()) throw new IllegalStateException("OpenAI moderation returned no category results");
        return moderation;
    }

    private static Map<String, Double> scores(List<ModerationResult> results) {
        Map<String, Double> scores = new LinkedHashMap<>();
        for (ModerationResult result : results) {
            if (result == null || result.getCategoryScores() == null) throw new IllegalStateException("OpenAI moderation returned incomplete category scores");
            CategoryScores score = result.getCategoryScores();
            merge(scores, "sexual", score.getSexual()); merge(scores, "hate", score.getHate());
            merge(scores, "harassment", score.getHarassment()); merge(scores, "self-harm", score.getSelfHarm());
            merge(scores, "sexual/minors", score.getSexualMinors()); merge(scores, "hate/threatening", score.getHateThreatening());
            merge(scores, "violence/graphic", score.getViolenceGraphic()); merge(scores, "self-harm/intent", score.getSelfHarmIntent());
            merge(scores, "self-harm/instructions", score.getSelfHarmInstructions()); merge(scores, "harassment/threatening", score.getHarassmentThreatening());
            merge(scores, "violence", score.getViolence());
        }
        return scores;
    }

    private static void merge(Map<String, Double> scores, String category, double score) {
        if (!Double.isFinite(score) || score < 0 || score > 1) throw new IllegalStateException("OpenAI moderation returned an invalid category score");
        scores.merge(category, score, Math::max);
    }
}
