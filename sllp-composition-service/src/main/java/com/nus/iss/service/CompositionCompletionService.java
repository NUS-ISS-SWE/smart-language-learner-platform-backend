package com.nus.iss.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

import com.nus.iss.persistence.ImprovementSuggestionListConverter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nus.iss.dto.CompositionGenerationCompletedEvent;
import com.nus.iss.contracts.grading.CompositionGradingCompletedEvent;
import com.nus.iss.dto.CompositionGradingProgressEvent;
import com.nus.iss.entity.Composition;
import com.nus.iss.entity.AiUsageEvent;
import com.nus.iss.enums.CompositionStatus;
import com.nus.iss.exception.InvalidCompositionRequestException;
import com.nus.iss.repository.CompositionRepository;
import com.nus.iss.repository.EvaluationRepository;
import com.nus.iss.repository.AiUsageEventRepository;

/** Applies asynchronous generation and grading outcomes to persisted compositions. */
@Service
public class CompositionCompletionService {
    private static final ImprovementSuggestionListConverter SUGGESTION_CONVERTER = new ImprovementSuggestionListConverter();
    private static final Map<String, String> STUDENT_SAFE_PROGRESS_MESSAGES = Map.of(
            "EVIDENCING", "Gathering supporting passages from your composition…",
            "GRADING", "Assessing your composition…",
            "REGRADING", "Refining your assessment…",
            "REVIEWING", "Checking the assessment for accuracy…",
            "SUGGESTING", "Creating personalised improvement suggestions…");

    private final CompositionRepository compositionRepository;
    private final EvaluationRepository evaluationRepository;
    private final AiUsageEventRepository aiUsageEventRepository;
    private final CompositionAccessService compositionAccessService;
    private final AssessmentQuotaClient assessmentQuotaClient;
    private final AgentReviewQueueService agentReviewQueue;

    public CompositionCompletionService(CompositionRepository compositionRepository,
                                        EvaluationRepository evaluationRepository,
                                        AiUsageEventRepository aiUsageEventRepository,
                                        CompositionAccessService compositionAccessService) {
        this(compositionRepository, evaluationRepository, aiUsageEventRepository, compositionAccessService, AssessmentQuotaClient.noop(), AgentReviewQueueService.noop());
    }

    public CompositionCompletionService(CompositionRepository compositionRepository,
                                        EvaluationRepository evaluationRepository,
                                        AiUsageEventRepository aiUsageEventRepository,
                                        CompositionAccessService compositionAccessService,
                                        AssessmentQuotaClient assessmentQuotaClient) {
        this(compositionRepository, evaluationRepository, aiUsageEventRepository, compositionAccessService, assessmentQuotaClient, AgentReviewQueueService.noop());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public CompositionCompletionService(CompositionRepository compositionRepository,
                                        EvaluationRepository evaluationRepository,
                                        AiUsageEventRepository aiUsageEventRepository,
                                        CompositionAccessService compositionAccessService,
                                        AssessmentQuotaClient assessmentQuotaClient,
                                        AgentReviewQueueService agentReviewQueue) {
        this.compositionRepository = compositionRepository;
        this.evaluationRepository = evaluationRepository;
        this.aiUsageEventRepository = aiUsageEventRepository;
        this.compositionAccessService = compositionAccessService;
        this.assessmentQuotaClient = assessmentQuotaClient;
        this.agentReviewQueue = agentReviewQueue;
    }

    @Transactional
    public void completeGrading(CompositionGradingCompletedEvent event) {
        validateGradingEvent(event);
        Composition composition = compositionAccessService.requireOwned(event.getUserId(), event.getCompositionId());
        LocalDateTime completedAt = event.getCompletedAt() == null ? LocalDateTime.now() : event.getCompletedAt();
        if (evaluationRepository.insertIfAbsent(event.getCompositionId(), event.getRequestId(), event.getRequestedAt(),
                event.getScore(), event.getFeedback(), event.getSuggestionStatus(), serializeSuggestions(event.getSuggestions()),
                event.isSuccess(), event.getFailureReason(), completedAt) == 0) {
            return;
        }
        composition.setCompositionStatus(event.isSuccess() ? CompositionStatus.REVIEWED : CompositionStatus.ASSESSMENT_FAILED);
        composition.setSystemFeedback(event.isSuccess() ? event.getFeedback() : event.getFailureReason());
        composition.setSuggestionStatus(event.isSuccess() ? event.getSuggestionStatus() : null);
        composition.setSuggestions(event.isSuccess() && event.getSuggestions() != null ? event.getSuggestions() : List.of());
        composition.setGradingProgressMessage(null);
        composition.setGradingProgressStage(null);
        composition.setUpdatedAt(completedAt);
        compositionRepository.save(composition);
        saveAiUsage(event, completedAt);
        if (!event.isSuccess()) {
            agentReviewQueue.recordFailure(event.getRequestId(), event.getCompositionId(), event.getUserId(), event.getFailureReason());
        }
        try {
            assessmentQuotaClient.settle(event.getRequestId(), event.isSuccess());
        } catch (RuntimeException ignored) {
            // A stale reservation expires safely; completion must never be replayed merely because quota settlement is down.
        }
    }

    private void saveAiUsage(CompositionGradingCompletedEvent event, LocalDateTime completedAt) {
        if (event.getRequestId() == null || event.getTotalTokens() == null
                || event.getAiProvider() == null || event.getAiModel() == null || event.getPricingVersion() == null
                || event.getInputPriceMicrosPerMillion() == null || event.getCachedInputPriceMicrosPerMillion() == null || event.getOutputPriceMicrosPerMillion() == null
                ) {
            return;
        }
        aiUsageEventRepository.save(AiUsageEvent.builder()
                .eventId(event.getRequestId()).requestId(event.getRequestId()).userId(event.getUserId()).compositionId(event.getCompositionId())
                .operation("COMPOSITION_GRADING").agent("grading").provider(event.getAiProvider()).model(event.getAiModel())
                .promptTokens(valueOrZero(event.getPromptTokens())).completionTokens(valueOrZero(event.getCompletionTokens()))
                .cachedInputTokens(valueOrZero(event.getCachedInputTokens())).totalTokens(valueOrZero(event.getTotalTokens()))
                .estimatedCostMicros(valueOrZero(event.getEstimatedCostMicros())).currency("USD")
                .pricingVersion(event.getPricingVersion()).inputPriceMicrosPerMillion(valueOrZero(event.getInputPriceMicrosPerMillion()))
                .cachedInputPriceMicrosPerMillion(valueOrZero(event.getCachedInputPriceMicrosPerMillion()))
                .outputPriceMicrosPerMillion(valueOrZero(event.getOutputPriceMicrosPerMillion())).createdAt(completedAt)
                .build());
    }

    private static long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }

    private static String serializeSuggestions(List<com.nus.iss.contracts.grading.ImprovementSuggestion> suggestions) {
        return SUGGESTION_CONVERTER.convertToDatabaseColumn(suggestions);
    }

    @Transactional
    public void recordGradingProgress(CompositionGradingProgressEvent event) {
        validateGradingProgressIdentity(event);
        Composition composition = compositionAccessService.requireOwned(event.getUserId(), event.getCompositionId());
        // Ignore delayed progress events after a completion has already been applied.
        if (composition.getCompositionStatus() != CompositionStatus.REVIEWING) return;
        validateGradingProgressPayload(event);
        composition.setGradingProgressMessage(event.getMessage());
        composition.setGradingProgressStage(event.getStage());
        composition.setUpdatedAt(event.getUpdatedAt() == null ? LocalDateTime.now() : event.getUpdatedAt());
        compositionRepository.save(composition);
    }

    @Transactional
    public void completeGeneration(CompositionGenerationCompletedEvent event) {
        validateGenerationEvent(event);
        Composition composition = compositionAccessService.requireOwned(event.getUserId(), event.getCompositionId());
        if (event.isSuccess()) {
            composition.setContent(event.getGeneratedContent());
            composition.setCompositionStatus(CompositionStatus.DRAFT);
            composition.setSystemFeedback(null);
            composition.setSuggestionStatus(null);
            composition.setSuggestions(List.of());
        } else {
            composition.setCompositionStatus(CompositionStatus.GENERATION_FAILED);
            composition.setSystemFeedback(event.getFailureReason());
        }
        composition.setUpdatedAt(event.getCompletedAt() == null ? LocalDateTime.now() : event.getCompletedAt());
        compositionRepository.save(composition);
    }

    private void validateGradingEvent(CompositionGradingCompletedEvent event) {
        if (event == null) throw new InvalidCompositionRequestException("Composition grading completed event is required");
        if (event.getRequestId() == null) {
            throw new InvalidCompositionRequestException("Composition grading request ID is required");
        }
        compositionAccessService.requireUserId(event.getUserId());
        compositionAccessService.requireCompositionId(event.getCompositionId());
        if (event.isSuccess() && (event.getFeedback() == null || event.getFeedback().isBlank())) {
            throw new InvalidCompositionRequestException("Composition grading feedback is required");
        }
        if (!event.isSuccess() && (event.getFailureReason() == null || event.getFailureReason().isBlank())) {
            throw new InvalidCompositionRequestException("Composition grading failure reason is required");
        }
    }

    private void validateGenerationEvent(CompositionGenerationCompletedEvent event) {
        if (event == null) throw new InvalidCompositionRequestException("Composition generation completed event is required");
        compositionAccessService.requireUserId(event.getUserId());
        compositionAccessService.requireCompositionId(event.getCompositionId());
        if (event.isSuccess() && (event.getGeneratedContent() == null || event.getGeneratedContent().isBlank())) {
            throw new InvalidCompositionRequestException("Generated composition content is required");
        }
        if (!event.isSuccess() && (event.getFailureReason() == null || event.getFailureReason().isBlank())) {
            throw new InvalidCompositionRequestException("Composition generation failure reason is required");
        }
    }

    private void validateGradingProgressIdentity(CompositionGradingProgressEvent event) {
        if (event == null) throw new InvalidCompositionRequestException("Composition grading progress event is required");
        compositionAccessService.requireUserId(event.getUserId());
        compositionAccessService.requireCompositionId(event.getCompositionId());
    }

    private void validateGradingProgressPayload(CompositionGradingProgressEvent event) {
        if (event.getMessage() == null || event.getMessage().isBlank()) {
            throw new InvalidCompositionRequestException("Composition grading progress message is required");
        }
        String expectedMessage = event.getStage() == null ? null : STUDENT_SAFE_PROGRESS_MESSAGES.get(event.getStage());
        if (!event.getMessage().equals(expectedMessage)) {
            throw new InvalidCompositionRequestException("Composition grading progress message is not supported");
        }
    }
}
