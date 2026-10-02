package com.nus.iss.service.impl;

import com.nus.iss.adapter.SllpAdminClient;
import com.nus.iss.adapter.SllpUserClient;
import com.nus.iss.config.SllpConstant;
import com.nus.iss.config.SllpProperties;
import com.nus.iss.dto.AdminRubricCriterionDTO;
import com.nus.iss.dto.AiGradingOutputDTO;
import com.nus.iss.dto.AiUsage;
import com.nus.iss.dto.GradingRequestDTO;
import com.nus.iss.dto.GradingResultDTO;
import com.nus.iss.dto.GradingScoreResult;
import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.GradingAttemptStatus;
import com.nus.iss.exception.A2aExecutionException;
import com.nus.iss.exception.A2aInvalidRequestException;
import com.nus.iss.model.GradingAttempt;
import com.nus.iss.repository.GradingRepository;
import com.nus.iss.service.AiUsageCostCalculator;
import com.nus.iss.service.GradingService;
import com.nus.iss.service.ModerationService;
import com.nus.iss.service.RubricCalculator;
import com.nus.iss.service.ValidationService;
import com.nus.iss.tool.AnchorTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
public class GradingServiceImpl implements GradingService {
    private final ChatClient chatClient;
    private final ValidationService validationService;
    private final RubricCalculator rubricCalculator;
    private final AiUsageCostCalculator aiUsageCostCalculator;
    private final GradingRepository gradingRepository;
    private final SllpProperties properties;
    private final ObjectMapper objectMapper;
    private final Resource gradingPromptTemplate;
    private final SllpUserClient sllpUserClient;
    private final SllpAdminClient sllpAdminClient;
    private final AnchorTools anchorTools;
    private final ModerationService moderationService;

    public GradingServiceImpl(ChatClient.Builder chatClientBuilder,
                              ValidationService validationService,
                              RubricCalculator rubricCalculator,
                              AiUsageCostCalculator aiUsageCostCalculator,
                              GradingRepository gradingRepository,
                              SllpProperties properties,
                              ObjectMapper objectMapper,
                              @Value("classpath:/promptTemplates/systemPromptTemplate.st")
                              Resource systemPromptTemplate,
                              @Value("classpath:/promptTemplates/gradingPromptTemplate.st")
                              Resource gradingPromptTemplate,
                              SllpUserClient sllpUserClient,
                              SllpAdminClient sllpAdminClient,
                              AnchorTools anchorTools,
                              ModerationService moderationService) {
        this.chatClient = chatClientBuilder.defaultSystem(systemPromptTemplate).build();
        this.validationService = validationService;
        this.rubricCalculator = rubricCalculator;
        this.aiUsageCostCalculator = aiUsageCostCalculator;
        this.gradingRepository = gradingRepository;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.gradingPromptTemplate = gradingPromptTemplate;
        this.sllpUserClient = sllpUserClient;
        this.sllpAdminClient = sllpAdminClient;
        this.anchorTools = anchorTools;
        this.moderationService = moderationService;
    }

    @Override
    public GradingResultDTO grade(GradingRequestDTO request) {
        long startedAt = System.nanoTime();
        log.info("Grading started requestId={} contextId={} compositionId={}", request.getRequestId(), request.getContextId(), request.getCompositionId());
        GradingAttempt attempt;
        try {
            attempt = acquireAttempt(request);
        } catch (A2aInvalidRequestException | A2aExecutionException error) {
            throw error;
        } catch (Exception error) {
            throw new A2aExecutionException(request.getRequestId(), "could not acquire grading audit attempt", error);
        }
        if (attempt.getStatus() == GradingAttemptStatus.COMPLETED) {
            log.info("Returning persisted grading result requestId={} attempt={} taskId={}",
                    request.getRequestId(), attempt.getAttemptNumber(), attempt.getTaskId());
            return savedResult(attempt);
        }
        try {
            DifficultyLevel difficultyLevel = sllpUserClient.getDifficultyLevel(request.getUserId());
            List<AdminRubricCriterionDTO> rubric = sllpAdminClient.getRubricCriteria(request.getLanguage(), difficultyLevel);
            validationService.validateRubric(rubric, request.getLanguage(), difficultyLevel);
            moderationService.moderateAndRecord(request);
            log.info("Invoking AI grading requestId={} attempt={} criteriaCount={} evidenceCount={}", request.getRequestId(), attempt.getAttemptNumber(), rubric.size(), request.getEvidence().size());
            ResponseEntity<ChatResponse, AiGradingOutputDTO> response = chatClient.prompt()
                    .user(user -> user.text(gradingPromptTemplate)
                            .param("title", request.getTitle())
                            .param("language", request.getLanguage().name())
                            .param("difficultyLevel", difficultyLevel.name())
                            .param("difficultyStandard", difficultyLevel.getGradingStandard())
                            .param("rubric", formatRubric(rubric))
                            .param("evidence", formatEvidence(request))
                            .param("content", request.getContent()))
                    .tools(anchorTools)
                    .toolContext(toolContext(request, attempt))
                    .call().responseEntity(AiGradingOutputDTO.class);

            AiGradingOutputDTO output = response.getEntity();
            logAiOutput(request, attempt, output);
            validationService.validateAiOutput(output, request, rubric);
            GradingScoreResult scoring = rubricCalculator.calculate(output.getCriteria(), rubric);
            ChatResponse chatResponse = response.getResponse();
            if (chatResponse == null || chatResponse.getMetadata() == null) {
                throw new IllegalStateException("AI response did not include metadata");
            }
            AiUsage usage = aiUsageCostCalculator.from(chatResponse.getMetadata().getUsage(),
                    chatResponse.getMetadata().getModel());
            GradingResultDTO result = GradingResultDTO.builder()
                    .taskId(attempt.getTaskId()).artifactId(attempt.getArtifactId())
                    .overallScore(scoring.overallScore())
                    .rubricScores(scoring.rubricScores())
                    .criteria(scoring.criteria()).confidence(output.getConfidence())
                    .modelVersion(usage.getModel()).promptVersion(SllpConstant.PROMPT_VERSION)
                    .usage(usage).build();
            complete(attempt, result);
            log.info("Grading completed requestId={} attempt={} taskId={} score={} confidence={} model={} totalTokens={} durationMs={}",
                    request.getRequestId(), attempt.getAttemptNumber(), attempt.getTaskId(),
                    result.getOverallScore(), result.getConfidence(), result.getModelVersion(),
                    usage.getTotalTokens(), elapsedMillis(startedAt));
            return result;
        } catch (A2aInvalidRequestException error) {
            fail(attempt, "INVALID_REQUEST", "structured grading request was invalid");
            log.warn("Grading rejected requestId={} attempt={} category={} durationMs={}",
                    request.getRequestId(), attempt.getAttemptNumber(),
                    error.getClass().getSimpleName(), elapsedMillis(startedAt));
            throw error;
        } catch (Exception error) {
            fail(attempt, error.getClass().getSimpleName(), "grading execution failed");
            log.warn("Grading failed requestId={} attempt={} category={} durationMs={}",
                    request.getRequestId(), attempt.getAttemptNumber(),
                    error.getClass().getSimpleName(), elapsedMillis(startedAt));
            throw new A2aExecutionException(request.getRequestId(), "grading execution failed", error);
        }
    }

    private GradingAttempt acquireAttempt(GradingRequestDTO request) {
        Optional<GradingAttempt> latest = gradingRepository.findFirstByRequestIdOrderByAttemptNumberDesc(request.getRequestId());
        if (latest.isPresent()) {
            GradingAttempt current = latest.get();
            if (!current.getRequestHash().equals(request.getRequestHash())) {
                log.warn("Request ID reuse detected with different input requestId={} latestAttempt={}",
                        request.getRequestId(), current.getAttemptNumber());
                throw new A2aInvalidRequestException(request.getRequestId(), "request ID was reused with different input");
            }
            if (current.getStatus() == GradingAttemptStatus.COMPLETED) return current;
            if (current.getStatus() == GradingAttemptStatus.IN_PROGRESS && !stale(current)) {
                log.info("Duplicate grading request is already in progress requestId={} attempt={}",
                        request.getRequestId(), current.getAttemptNumber());
                throw new A2aExecutionException(request.getRequestId(), "grading request is already in progress");
            }
            if (current.getStatus() == GradingAttemptStatus.IN_PROGRESS) {
                log.warn("Marking stale grading attempt as failed requestId={} attempt={}",
                        request.getRequestId(), current.getAttemptNumber());
                current.setStatus(GradingAttemptStatus.FAILED);
                current.setCompletedAt(now());
                current.setErrorCode("STALE_ATTEMPT");
                current.setErrorMessage("grading attempt exceeded its processing timeout");
                gradingRepository.saveAndFlush(current);
            }
        }
        int attemptNumber = latest.map(item -> item.getAttemptNumber() + 1).orElse(1);
        GradingAttempt created = GradingAttempt.builder().id(UUID.randomUUID())
                .requestId(request.getRequestId()).contextId(request.getContextId())
                .requestHash(request.getRequestHash()).attemptNumber(attemptNumber)
                .status(GradingAttemptStatus.IN_PROGRESS).compositionId(request.getCompositionId())
                .userId(request.getUserId()).taskId(UUID.randomUUID().toString())
                .artifactId(UUID.randomUUID().toString()).receivedAt(now()).build();
        try {
            GradingAttempt saved = gradingRepository.saveAndFlush(created);
            log.info("Grading audit attempt created requestId={} attempt={} taskId={}",
                    saved.getRequestId(), saved.getAttemptNumber(), saved.getTaskId());
            return saved;
        } catch (DataIntegrityViolationException race) {
            log.info("Concurrent grading attempt detected requestId={} attempt={}",
                    request.getRequestId(), attemptNumber);
            GradingAttempt winner = gradingRepository.findFirstByRequestIdOrderByAttemptNumberDesc(request.getRequestId())
                    .orElseThrow(() -> race);
            if (winner.getStatus() == GradingAttemptStatus.COMPLETED
                    && winner.getRequestHash().equals(request.getRequestHash())) return winner;
            throw new A2aExecutionException(request.getRequestId(), "grading request is already in progress", race);
        }
    }

    private void complete(GradingAttempt attempt, GradingResultDTO result) {
        try {
            AiUsage usage = result.getUsage();
            attempt.setStatus(GradingAttemptStatus.COMPLETED);
            attempt.setCompletedAt(now());
            attempt.setModelVersion(result.getModelVersion());
            attempt.setPromptVersion(result.getPromptVersion());
            attempt.setPromptTokens(usage.getPromptTokens());
            attempt.setCompletionTokens(usage.getCompletionTokens());
            attempt.setCachedInputTokens(usage.getCachedInputTokens());
            attempt.setTotalTokens(usage.getTotalTokens());
            attempt.setEstimatedCostMicros(usage.getEstimatedCostMicros());
            attempt.setResultPayload(objectMapper.writeValueAsString(result));
            gradingRepository.saveAndFlush(attempt);
            log.info("Completed grading audit persisted requestId={} attempt={}",
                    attempt.getRequestId(), attempt.getAttemptNumber());
        } catch (Exception error) {
            throw new IllegalStateException("could not persist completed grading attempt", error);
        }
    }

    private void fail(GradingAttempt attempt, String code, String message) {
        try {
            attempt.setStatus(GradingAttemptStatus.FAILED);
            attempt.setCompletedAt(now());
            attempt.setErrorCode(code == null ? "GRADING_FAILURE" : code.substring(0, Math.min(code.length(), 100)));
            attempt.setErrorMessage(message);
            gradingRepository.saveAndFlush(attempt);
            log.info("Failed grading audit persisted requestId={} attempt={} errorCode={}",
                    attempt.getRequestId(), attempt.getAttemptNumber(), attempt.getErrorCode());
        } catch (Exception persistenceError) {
            log.error("Could not persist failed grading attempt requestId={}", attempt.getRequestId(), persistenceError);
        }
    }

    private GradingResultDTO savedResult(GradingAttempt attempt) {
        try {
            GradingResultDTO result = objectMapper.readValue(attempt.getResultPayload(), GradingResultDTO.class);
            result.setTaskId(attempt.getTaskId());
            result.setArtifactId(attempt.getArtifactId());
            return result;
        } catch (Exception error) {
            throw new A2aExecutionException(attempt.getRequestId(), "saved grading result is invalid", error);
        }
    }

    private boolean stale(GradingAttempt attempt) {
        return attempt.getReceivedAt().plus(properties.getAudit().getInProgressTimeout()).isBefore(now());
    }

    private static String formatRubric(List<AdminRubricCriterionDTO> rubric) {
        return rubric.stream()
                .map(item -> "- %s (%s%%): %s".formatted(
                        item.criterionName(), item.weightPercentage(), item.criterionDescription()))
                .collect(Collectors.joining("\n"));
    }

    private static String formatEvidence(GradingRequestDTO request) {
        if (request.getEvidence().isEmpty()) return "No evidence supplied; inspect the composition directly.";
        return IntStream.range(0, request.getEvidence().size())
                .mapToObj(index -> "%d. %s".formatted(index + 1, request.getEvidence().get(index)))
                .collect(Collectors.joining("\n"));
    }

    private static Map<String, Object> toolContext(GradingRequestDTO request, GradingAttempt attempt) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put(AnchorTools.REQUEST_ID_CONTEXT_KEY, request.getRequestId());
        if (request.getCompositionId() != null) {
            context.put(AnchorTools.COMPOSITION_ID_CONTEXT_KEY, request.getCompositionId());
        }
        context.put(AnchorTools.ATTEMPT_CONTEXT_KEY, attempt.getAttemptNumber());
        return context;
    }

    private void logAiOutput(GradingRequestDTO request, GradingAttempt attempt, AiGradingOutputDTO output) {
        try {
            log.info("AI grading output received requestId={} compositionId={} attempt={} output={}",
                    request.getRequestId(), request.getCompositionId(), attempt.getAttemptNumber(),
                    objectMapper.writeValueAsString(output));
        } catch (Exception error) {
            log.warn("Could not serialize AI grading output for logging requestId={} compositionId={} attempt={} category={}",
                    request.getRequestId(), request.getCompositionId(), attempt.getAttemptNumber(),
                    error.getClass().getSimpleName());
        }
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private static long elapsedMillis(long startedAt) {
        return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }
}
