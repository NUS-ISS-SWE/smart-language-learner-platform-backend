package com.nus.iss.service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import com.nus.iss.contracts.grading.CompositionGradingRequestedEvent;
import com.nus.iss.dto.CompositionGenerationRequestedEvent;
import com.nus.iss.dto.CompositionResponse;
import com.nus.iss.dto.CreateCompositionRequest;
import com.nus.iss.dto.GenerateCompositionRequest;
import com.nus.iss.dto.UpdateCompositionRequest;
import com.nus.iss.entity.Composition;
import com.nus.iss.enums.CompositionSource;
import com.nus.iss.enums.CompositionStatus;
import com.nus.iss.enums.Language;
import com.nus.iss.exception.InvalidCompositionRequestException;
import com.nus.iss.exception.CompositionModeratedException;
import com.nus.iss.repository.CompositionRepository;

import lombok.extern.slf4j.Slf4j;

/** Handles all state-changing learner composition workflows. */
@Slf4j
@Service
public class CompositionCommandService {
    private final CompositionRepository compositionRepository;
    private final CompositionAccessService compositionAccessService;
    private final CompositionMapper compositionMapper;
    private final LanguageDetectorService languageDetectorService;
    private final KafkaProducerService kafkaProducerService;
    private final AssessmentQuotaClient assessmentQuotaClient;

    public CompositionCommandService(CompositionRepository compositionRepository,
                                     CompositionAccessService compositionAccessService,
                                     CompositionMapper compositionMapper,
                                     LanguageDetectorService languageDetectorService,
                                     KafkaProducerService kafkaProducerService) {
        this(compositionRepository, compositionAccessService, compositionMapper, languageDetectorService, kafkaProducerService, AssessmentQuotaClient.noop());
    }

    @Autowired
    public CompositionCommandService(CompositionRepository compositionRepository,
                                     CompositionAccessService compositionAccessService,
                                     CompositionMapper compositionMapper,
                                     LanguageDetectorService languageDetectorService,
                                     KafkaProducerService kafkaProducerService,
                                     AssessmentQuotaClient assessmentQuotaClient) {
        this.compositionRepository = compositionRepository;
        this.compositionAccessService = compositionAccessService;
        this.compositionMapper = compositionMapper;
        this.languageDetectorService = languageDetectorService;
        this.kafkaProducerService = kafkaProducerService;
        this.assessmentQuotaClient = assessmentQuotaClient;
    }

    @Transactional
    public CompositionResponse create(Long userId, CreateCompositionRequest request) {
        compositionAccessService.requireUserId(userId);
        validateCreateRequest(request);
        Language language = detectLanguage(request.content());
        LocalDateTime now = LocalDateTime.now();
        Composition composition = Composition.builder()
                .userId(userId).title(request.title()).content(request.content())
                .compositionSource(CompositionSource.RAW_TEXT).language(language)
                .compositionStatus(CompositionStatus.DRAFT).sourceDocumentId(request.sourceDocumentId())
                .createdAt(now).updatedAt(now).build();
        return compositionMapper.toResponse(compositionRepository.save(composition));
    }

    @Transactional
    public CompositionResponse update(Long userId, Long compositionId, UpdateCompositionRequest request) {
        validateUpdateRequest(request);
        Composition composition = compositionAccessService.requireOwned(userId, compositionId);
        requireActive(composition);
        composition.setTitle(request.title());
        composition.setContent(request.content());
        composition.setLanguage(detectLanguage(request.content()));
        composition.setCompositionStatus(CompositionStatus.DRAFT);
        composition.setSystemFeedback(null);
        composition.setSuggestionStatus(null);
        composition.setSuggestions(List.of());
        composition.setGradingProgressMessage(null);
        composition.setGradingProgressStage(null);
        composition.setUpdatedAt(LocalDateTime.now());
        return compositionMapper.toResponse(compositionRepository.save(composition));
    }

    @Transactional
    public void delete(Long userId, Long compositionId) {
        Composition composition = compositionAccessService.requireOwned(userId, compositionId);
        requireActive(composition);
        compositionRepository.delete(composition);
        log.info("Deleted composition with ID: {} for user: {}", compositionId, userId);
    }

    @Transactional
    public CompositionResponse requestGrading(Long userId, Long compositionId) {
        Composition composition = compositionAccessService.requireOwned(userId, compositionId);
        requireActive(composition);
        if (composition.getCompositionStatus() != CompositionStatus.DRAFT
                && composition.getCompositionStatus() != CompositionStatus.ASSESSMENT_FAILED) {
            throw new InvalidCompositionRequestException("Only draft or failed-assessment compositions can be submitted for review");
        }
        UUID requestId = UUID.randomUUID();
        assessmentQuotaClient.reserve(userId, requestId);
        LocalDateTime now = LocalDateTime.now();
        composition.setCompositionStatus(CompositionStatus.REVIEWING);
        composition.setSystemFeedback(null);
        composition.setSuggestionStatus(null);
        composition.setSuggestions(List.of());
        composition.setGradingProgressMessage("Preparing your assessment…");
        composition.setGradingProgressStage("PREPARING");
        composition.setUpdatedAt(now);
        Composition saved = compositionRepository.save(composition);
        try {
            kafkaProducerService.sendCompositionGradingRequestedEvent(CompositionGradingRequestedEvent.builder()
                    .requestId(requestId).compositionId(saved.getId()).userId(saved.getUserId()).title(saved.getTitle()).content(saved.getContent())
                    .language(saved.getLanguage().name()).requestedAt(now).build());
        } catch (RuntimeException error) {
            // The database transaction rolls back to DRAFT; release the external hold now
            // rather than forcing the learner to wait for the reservation expiry.
            try {
                assessmentQuotaClient.settle(requestId, false);
            } catch (RuntimeException ignored) {
                // The reservation has its own expiry as a final recovery path.
            }
            throw error;
        }
        return compositionMapper.toResponse(saved);
    }

    @Transactional
    public CompositionResponse generate(Long userId, GenerateCompositionRequest request) {
        compositionAccessService.requireUserId(userId);
        validateGenerateRequest(request);
        LocalDateTime now = LocalDateTime.now();
        Composition composition = Composition.builder()
                .userId(userId).title(request.title())
                .compositionSource(CompositionSource.SYSTEM).language(languageDetectorService.detectLanguage(request.title()).orElse(Language.ENGLISH))
                .compositionStatus(CompositionStatus.GENERATING).createdAt(now).updatedAt(now).build();
        Composition saved = compositionRepository.save(composition);
        kafkaProducerService.sendCompositionGenerationRequestedEvent(CompositionGenerationRequestedEvent.builder()
                .compositionId(saved.getId()).userId(saved.getUserId()).title(saved.getTitle())
                .language(saved.getLanguage()).requestedAt(now).build());
        return compositionMapper.toResponse(saved);
    }

    private Language detectLanguage(String content) {
        return languageDetectorService.detectLanguage(content)
                .orElseThrow(() -> new InvalidCompositionRequestException("Only English and Chinese compositions are supported"));
    }

    private static void validateCreateRequest(CreateCompositionRequest request) {
        if (request == null) throw new InvalidCompositionRequestException("Composition request body is required");
        validateTitle(request.title());
        validateContent(request.content());
    }

    private static void validateUpdateRequest(UpdateCompositionRequest request) {
        if (request == null) throw new InvalidCompositionRequestException("Composition request body is required");
        validateTitle(request.title());
        validateContent(request.content());
    }

    private static void validateGenerateRequest(GenerateCompositionRequest request) {
        if (request == null) throw new InvalidCompositionRequestException("Composition generation request body is required");
        validateTitle(request.title());
    }

    private static void validateTitle(String title) {
        if (title == null || title.isBlank()) throw new InvalidCompositionRequestException("Composition title is required");
    }

    private static void validateContent(String content) {
        if (content == null || content.isBlank()) throw new InvalidCompositionRequestException("Composition content is required");
    }

    private static void requireActive(Composition composition) {
        if (composition.getModerationStatus() != null
                && composition.getModerationStatus() != com.nus.iss.enums.ModerationStatus.ACTIVE) {
            throw new CompositionModeratedException();
        }
    }
}
