package com.nus.iss.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nus.iss.dto.CompositionDTO;
import com.nus.iss.dto.CompositionGenerationCompletedEvent;
import com.nus.iss.dto.CompositionGenerationRequestedEvent;
import com.nus.iss.dto.CompositionGradingCompletedEvent;
import com.nus.iss.dto.CompositionGradingRequestedEvent;
import com.nus.iss.entity.Composition;
import com.nus.iss.entity.Evaluation;
import com.nus.iss.enums.CompositionSource;
import com.nus.iss.enums.CompositionStatus;
import com.nus.iss.enums.Language;
import com.nus.iss.repository.CompositionRepository;
import com.nus.iss.repository.EvaluationRepository;
import com.nus.iss.service.CompositionService;
import com.nus.iss.service.KafkaProducerService;
import com.nus.iss.service.LanguageDetectorService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CompositionServiceImpl implements CompositionService {

    private final CompositionRepository compositionRepository;
    private final EvaluationRepository evaluationRepository;
    private final LanguageDetectorService languageDetectorService;
    private final KafkaProducerService kafkaProducerService;

    @Autowired
    public CompositionServiceImpl(CompositionRepository compositionRepository,
                                  EvaluationRepository evaluationRepository,
                                  LanguageDetectorService languageDetectorService,
                                  KafkaProducerService kafkaProducerService) {
        this.compositionRepository = compositionRepository;
        this.evaluationRepository = evaluationRepository;
        this.languageDetectorService = languageDetectorService;
        this.kafkaProducerService = kafkaProducerService;
    }

    @Override
    @Transactional
    public CompositionDTO createNewComposition(Long userId, CompositionDTO compositionDTO) {
        validateCreateRequest(userId, compositionDTO);

        LocalDateTime now = LocalDateTime.now();
        Language detectedLanguage = languageDetectorService.detectLanguage(compositionDTO.getContent())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Only English and Chinese compositions are supported"));

        Composition composition = Composition.builder()
                .userId(userId)
                .title(compositionDTO.getTitle())
                .content(compositionDTO.getContent())
                .compositionSource(CompositionSource.RAW_TEXT)
                .language(detectedLanguage)
                .compositionStatus(CompositionStatus.DRAFT)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Composition savedComposition = compositionRepository.save(composition);
        log.info("Created composition with ID: {} for user: {}", savedComposition.getId(), userId);

        return mapToDTO(savedComposition);
    }

    @Override
    public List<CompositionDTO> getAllCompositionByUserId(Long userId, Pageable pageable) {
        validateUserId(userId);
        validatePageable(pageable);

        List<Composition> compositions = compositionRepository.findByUserId(userId, pageable);
        log.info("Found {} compositions for user: {}", compositions.size(), userId);

        return compositions.stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    public CompositionDTO getSingleCompositionById(Long userId, Long id) {
        validateUserId(userId);
        validateCompositionId(id);

        Composition composition = compositionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NoSuchElementException("Composition not found"));

        return mapToDTO(composition);
    }

    @Override
    public CompositionDTO getCompositionByIdForAdmin(Long id) {
        validateCompositionId(id);
        Composition composition = compositionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Composition not found"));
        return mapToDTO(composition);
    }

    @Override
    @Transactional
    public CompositionDTO updateComposition(Long userId, Long id, CompositionDTO compositionDTO) {
        validateCreateRequest(userId, compositionDTO);
        validateCompositionId(id);

        Composition composition = compositionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NoSuchElementException("Composition not found"));

        Language detectedLanguage = languageDetectorService.detectLanguage(compositionDTO.getContent())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Only English and Chinese compositions are supported"));

        composition.setTitle(compositionDTO.getTitle());
        composition.setContent(compositionDTO.getContent());
        composition.setLanguage(detectedLanguage);
        composition.setCompositionStatus(CompositionStatus.DRAFT);
        composition.setUpdatedAt(LocalDateTime.now());

        Composition savedComposition = compositionRepository.save(composition);
        log.info("Updated composition with ID: {} for user: {}", savedComposition.getId(), userId);

        return mapToDTO(savedComposition);
    }

    @Override
    @Transactional
    public void deleteComposition(Long userId, Long id) {
        validateUserId(userId);
        validateCompositionId(id);

        Composition composition = compositionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NoSuchElementException("Composition not found"));

        compositionRepository.delete(composition);
        log.info("Deleted composition with ID: {} for user: {}", id, userId);
    }

    @Override
    @Transactional
    public CompositionDTO requestToGradeComposition(Long userId, Long id) {
        validateUserId(userId);
        validateCompositionId(id);

        Composition composition = compositionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NoSuchElementException("Composition not found"));

        if (composition.getCompositionStatus() != CompositionStatus.DRAFT) {
            throw new IllegalArgumentException("Only draft compositions can be submitted for review");
        }

        LocalDateTime now = LocalDateTime.now();
        composition.setCompositionStatus(CompositionStatus.REVIEWING);
        composition.setUpdatedAt(now);
        Composition savedComposition = compositionRepository.save(composition);

        CompositionGradingRequestedEvent event = CompositionGradingRequestedEvent.builder()
                .compositionId(savedComposition.getId())
                .userId(savedComposition.getUserId())
                .title(savedComposition.getTitle())
                .content(savedComposition.getContent())
                .language(savedComposition.getLanguage())
                .requestedAt(now)
                .build();
        kafkaProducerService.sendCompositionGradingRequestedEvent(event);

        log.info("Requested grading for composition with ID: {} for user: {}", id, userId);
        return mapToDTO(savedComposition);
    }

    @Override
    @Transactional
    public void completeCompositionGrading(CompositionGradingCompletedEvent event) {
        validateGradingCompletedEvent(event);

        Composition composition = compositionRepository.findByIdAndUserId(event.getCompositionId(), event.getUserId())
                .orElseThrow(() -> new NoSuchElementException("Composition not found"));

        if (event.isSuccess()) {
            composition.setCompositionStatus(CompositionStatus.REVIEWED);
            composition.setSystemFeedback(event.getFeedback());
        } else {
            composition.setCompositionStatus(CompositionStatus.DRAFT);
            composition.setSystemFeedback(event.getFailureReason());
        }

        LocalDateTime completedAt = event.getCompletedAt() != null ? event.getCompletedAt() : LocalDateTime.now();
        composition.setUpdatedAt(completedAt);
        compositionRepository.save(composition);

        Evaluation evaluation = Evaluation.builder()
                .compositionId(event.getCompositionId())
                .score(event.getScore())
                .feedback(event.getFeedback())
                .success(event.isSuccess())
                .failureReason(event.getFailureReason())
                .evaluatedAt(completedAt)
                .build();
        evaluationRepository.save(evaluation);

        log.info("Completed grading update for composition with ID: {}, success: {}",
                event.getCompositionId(), event.isSuccess());
    }

    @Override
    @Transactional
    public CompositionDTO generateComposition(Long userId, CompositionDTO compositionDTO) {
        validateGenerateRequest(userId, compositionDTO);

        LocalDateTime now = LocalDateTime.now();
        Language detectedLanguage = languageDetectorService.detectLanguage(compositionDTO.getTitle())
                .orElse(Language.ENGLISH);

        Composition composition = Composition.builder()
                .userId(userId)
                .title(compositionDTO.getTitle())
                .compositionSource(CompositionSource.SYSTEM)
                .language(detectedLanguage)
                .compositionStatus(CompositionStatus.GENERATING)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Composition savedComposition = compositionRepository.save(composition);

        CompositionGenerationRequestedEvent event = CompositionGenerationRequestedEvent.builder()
                .compositionId(savedComposition.getId())
                .userId(savedComposition.getUserId())
                .title(savedComposition.getTitle())
                .language(savedComposition.getLanguage())
                .requestedAt(now)
                .build();
        kafkaProducerService.sendCompositionGenerationRequestedEvent(event);

        log.info("Requested generation for composition with ID: {} for user: {}",
                savedComposition.getId(), userId);
        return mapToDTO(savedComposition);
    }

    @Override
    @Transactional
    public void completeCompositionGeneration(CompositionGenerationCompletedEvent event) {
        validateGenerationCompletedEvent(event);

        Composition composition = compositionRepository.findByIdAndUserId(
                        event.getCompositionId(), event.getUserId())
                .orElseThrow(() -> new NoSuchElementException("Composition not found"));

        if (event.isSuccess()) {
            composition.setContent(event.getGeneratedContent());
            composition.setCompositionStatus(CompositionStatus.DRAFT);
            composition.setSystemFeedback(null);
        } else {
            composition.setCompositionStatus(CompositionStatus.GENERATION_FAILED);
            composition.setSystemFeedback(event.getFailureReason());
        }

        composition.setUpdatedAt(event.getCompletedAt() != null ? event.getCompletedAt() : LocalDateTime.now());
        compositionRepository.save(composition);
        log.info("Completed generation update for composition with ID: {}, success: {}",
                event.getCompositionId(), event.isSuccess());
    }

    private void validateCreateRequest(Long userId, CompositionDTO compositionDTO) {
        validateUserId(userId);
        if (compositionDTO == null) {
            throw new IllegalArgumentException("Composition request body is required");
        }
        if (compositionDTO.getTitle() == null || compositionDTO.getTitle().isBlank()) {
            throw new IllegalArgumentException("Composition title is required");
        }
        if (compositionDTO.getContent() == null || compositionDTO.getContent().isBlank()) {
            throw new IllegalArgumentException("Composition content is required");
        }
    }

    private void validateGenerateRequest(Long userId, CompositionDTO compositionDTO) {
        validateUserId(userId);
        if (compositionDTO == null) {
            throw new IllegalArgumentException("Composition generation request body is required");
        }
        if (compositionDTO.getTitle() == null || compositionDTO.getTitle().isBlank()) {
            throw new IllegalArgumentException("Composition generation title is required");
        }
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId < 1) {
            throw new IllegalArgumentException("Valid user ID header is required");
        }
    }

    private void validatePageable(Pageable pageable) {
        if (pageable == null) {
            throw new IllegalArgumentException("Pagination information is required");
        }
        if (pageable.getPageNumber() < 0) {
            throw new IllegalArgumentException("Page number must not be negative");
        }
        if (pageable.getPageSize() < 1 || pageable.getPageSize() > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }
    }

    private void validateCompositionId(Long id) {
        if (id == null || id < 1) {
            throw new IllegalArgumentException("Composition ID is required");
        }
    }

    private void validateGradingCompletedEvent(CompositionGradingCompletedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Composition grading completed event is required");
        }
        validateUserId(event.getUserId());
        validateCompositionId(event.getCompositionId());
        if (event.isSuccess() && (event.getFeedback() == null || event.getFeedback().isBlank())) {
            throw new IllegalArgumentException("Composition grading feedback is required");
        }
        if (!event.isSuccess() && (event.getFailureReason() == null || event.getFailureReason().isBlank())) {
            throw new IllegalArgumentException("Composition grading failure reason is required");
        }
    }

    private void validateGenerationCompletedEvent(CompositionGenerationCompletedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Composition generation completed event is required");
        }
        validateUserId(event.getUserId());
        validateCompositionId(event.getCompositionId());
        if (event.isSuccess() && (event.getGeneratedContent() == null || event.getGeneratedContent().isBlank())) {
            throw new IllegalArgumentException("Generated composition content is required");
        }
        if (!event.isSuccess() && (event.getFailureReason() == null || event.getFailureReason().isBlank())) {
            throw new IllegalArgumentException("Composition generation failure reason is required");
        }
    }

    private CompositionDTO mapToDTO(Composition composition) {
        return CompositionDTO.builder()
                .id(composition.getId())
                .userId(composition.getUserId())
                .title(composition.getTitle())
                .content(composition.getContent())
                .compositionSource(composition.getCompositionSource())
                .language(composition.getLanguage())
                .compositionStatus(composition.getCompositionStatus())
                .systemFeedback(composition.getSystemFeedback())
                .createdAt(composition.getCreatedAt())
                .updatedAt(composition.getUpdatedAt())
                .build();
    }
}
