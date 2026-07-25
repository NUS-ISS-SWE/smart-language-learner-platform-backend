package com.nus.iss.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.nus.iss.dto.CompositionDTO;
import com.nus.iss.dto.CompositionGenerationCompletedEvent;
import com.nus.iss.dto.CompositionGenerationRequestedEvent;
import com.nus.iss.dto.CompositionGradingCompletedEvent;
import com.nus.iss.dto.CompositionGradingRequestedEvent;
import com.nus.iss.entity.Composition;
import com.nus.iss.enums.CompositionSource;
import com.nus.iss.enums.CompositionStatus;
import com.nus.iss.enums.Language;
import com.nus.iss.repository.CompositionRepository;
import com.nus.iss.repository.EvaluationRepository;
import com.nus.iss.service.KafkaProducerService;
import com.nus.iss.service.LanguageDetectorService;

@ExtendWith(MockitoExtension.class)
class CompositionServiceImplTest {

    @Mock
    private CompositionRepository compositionRepository;

    @Mock
    private EvaluationRepository evaluationRepository;

    @Mock
    private LanguageDetectorService languageDetectorService;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @InjectMocks
    private CompositionServiceImpl compositionService;

    @Test
    void createNewComposition_whenRequestIsValidSavesDraftRawTextComposition() {
        CompositionDTO request = compositionRequest();
        when(languageDetectorService.detectLanguage("This is my composition.")).thenReturn(Optional.of(Language.ENGLISH));
        when(compositionRepository.save(any(Composition.class))).thenAnswer(invocation -> {
            Composition composition = invocation.getArgument(0);
            composition.setId(10L);
            return composition;
        });

        CompositionDTO response = compositionService.createNewComposition(1L, request);

        ArgumentCaptor<Composition> compositionCaptor = ArgumentCaptor.forClass(Composition.class);
        verify(compositionRepository).save(compositionCaptor.capture());
        Composition savedComposition = compositionCaptor.getValue();

        assertEquals(10L, response.getId());
        assertEquals(1L, response.getUserId());
        assertEquals("My title", response.getTitle());
        assertEquals("This is my composition.", response.getContent());
        assertEquals(CompositionSource.RAW_TEXT, response.getCompositionSource());
        assertEquals(Language.ENGLISH, response.getLanguage());
        assertEquals(CompositionStatus.DRAFT, response.getCompositionStatus());
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getUpdatedAt());
        assertEquals(savedComposition.getCreatedAt(), response.getCreatedAt());
        assertEquals(savedComposition.getUpdatedAt(), response.getUpdatedAt());
        verifyNoInteractions(kafkaProducerService);
    }

    @Test
    void createNewComposition_whenLanguageIsUnsupportedThrowsIllegalArgumentException() {
        CompositionDTO request = compositionRequest();
        when(languageDetectorService.detectLanguage("This is my composition.")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> compositionService.createNewComposition(1L, request));

        verify(compositionRepository, never()).save(any(Composition.class));
        verifyNoInteractions(kafkaProducerService);
    }

    @Test
    void createNewComposition_whenUsernameIsBlankThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> compositionService.createNewComposition(0L, compositionRequest()));

        verifyNoInteractions(languageDetectorService, compositionRepository, kafkaProducerService);
    }

    @Test
    void createNewComposition_whenUsernameIsNullThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> compositionService.createNewComposition(null, compositionRequest()));

        verifyNoInteractions(languageDetectorService, compositionRepository, kafkaProducerService);
    }

    @Test
    void createNewComposition_whenRequestBodyIsNullThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> compositionService.createNewComposition(1L, null));

        verifyNoInteractions(languageDetectorService, compositionRepository, kafkaProducerService);
    }

    @Test
    void createNewComposition_whenTitleIsBlankThrowsIllegalArgumentException() {
        CompositionDTO request = compositionRequest();
        request.setTitle(" ");

        assertThrows(IllegalArgumentException.class,
                () -> compositionService.createNewComposition(1L, request));

        verifyNoInteractions(languageDetectorService, compositionRepository, kafkaProducerService);
    }

    @Test
    void createNewComposition_whenTitleIsNullThrowsIllegalArgumentException() {
        CompositionDTO request = compositionRequest();
        request.setTitle(null);

        assertThrows(IllegalArgumentException.class,
                () -> compositionService.createNewComposition(1L, request));

        verifyNoInteractions(languageDetectorService, compositionRepository, kafkaProducerService);
    }

    @Test
    void createNewComposition_whenContentIsBlankThrowsIllegalArgumentException() {
        CompositionDTO request = compositionRequest();
        request.setContent(" ");

        assertThrows(IllegalArgumentException.class,
                () -> compositionService.createNewComposition(1L, request));

        verifyNoInteractions(languageDetectorService, compositionRepository, kafkaProducerService);
    }

    @Test
    void createNewComposition_whenContentIsNullThrowsIllegalArgumentException() {
        CompositionDTO request = compositionRequest();
        request.setContent(null);

        assertThrows(IllegalArgumentException.class,
                () -> compositionService.createNewComposition(1L, request));

        verifyNoInteractions(languageDetectorService, compositionRepository, kafkaProducerService);
    }

    @Test
    void getAllCompositionByUserId_whenRequestIsValidReturnsMappedCompositions() {
        Pageable pageable = PageRequest.of(0, 10);
        Composition composition = persistedComposition();
        when(compositionRepository.findByUserId(1L, pageable)).thenReturn(List.of(composition));

        List<CompositionDTO> response = compositionService.getAllCompositionByUserId(1L, pageable);

        assertEquals(1, response.size());
        assertEquals(10L, response.get(0).getId());
        assertEquals(1L, response.get(0).getUserId());
        assertEquals("My title", response.get(0).getTitle());
    }

    @Test
    void getAllCompositionByUserId_whenPageableIsNullThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> compositionService.getAllCompositionByUserId(1L, null));

        verifyNoInteractions(compositionRepository);
    }

    @Test
    void getAllCompositionByUserId_whenPageNumberIsNegativeThrowsIllegalArgumentException() {
        Pageable pageable = org.mockito.Mockito.mock(Pageable.class);
        when(pageable.getPageNumber()).thenReturn(-1);

        assertThrows(IllegalArgumentException.class,
                () -> compositionService.getAllCompositionByUserId(1L, pageable));

        verifyNoInteractions(compositionRepository);
    }

    @Test
    void getAllCompositionByUserId_whenPageSizeIsLessThanOneThrowsIllegalArgumentException() {
        Pageable pageable = org.mockito.Mockito.mock(Pageable.class);
        when(pageable.getPageNumber()).thenReturn(0);
        when(pageable.getPageSize()).thenReturn(0);

        assertThrows(IllegalArgumentException.class,
                () -> compositionService.getAllCompositionByUserId(1L, pageable));

        verifyNoInteractions(compositionRepository);
    }

    @Test
    void getAllCompositionByUserId_whenPageSizeIsTooLargeThrowsIllegalArgumentException() {
        Pageable pageable = PageRequest.of(0, 101);

        assertThrows(IllegalArgumentException.class,
                () -> compositionService.getAllCompositionByUserId(1L, pageable));

        verifyNoInteractions(compositionRepository);
    }

    @Test
    void getSingleCompositionById_whenCompositionExistsReturnsMappedComposition() {
        Composition composition = persistedComposition();
        when(compositionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(composition));

        CompositionDTO response = compositionService.getSingleCompositionById(1L, 10L);

        assertEquals(10L, response.getId());
        assertEquals(1L, response.getUserId());
        assertEquals(CompositionStatus.DRAFT, response.getCompositionStatus());
    }

    @Test
    void getSingleCompositionById_whenCompositionDoesNotExistThrowsNoSuchElementException() {
        when(compositionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> compositionService.getSingleCompositionById(1L, 10L));
    }

    @Test
    void getSingleCompositionById_whenIdIsInvalidThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> compositionService.getSingleCompositionById(1L, 0L));

        verifyNoInteractions(compositionRepository);
    }

    @Test
    void getSingleCompositionById_whenIdIsNullThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> compositionService.getSingleCompositionById(1L, null));

        verifyNoInteractions(compositionRepository);
    }

    @Test
    void updateComposition_whenRequestIsValidUpdatesExistingComposition() {
        Composition composition = persistedComposition();
        CompositionDTO request = CompositionDTO.builder()
                .title("Updated title")
                .content("Updated content")
                .build();
        when(compositionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(composition));
        when(languageDetectorService.detectLanguage("Updated content")).thenReturn(Optional.of(Language.CHINESE));
        when(compositionRepository.save(composition)).thenReturn(composition);

        CompositionDTO response = compositionService.updateComposition(1L, 10L, request);

        assertEquals("Updated title", response.getTitle());
        assertEquals("Updated content", response.getContent());
        assertEquals(Language.CHINESE, response.getLanguage());
        assertEquals(CompositionStatus.DRAFT, response.getCompositionStatus());
        verify(compositionRepository).save(composition);
    }

    @Test
    void updateComposition_whenCompositionDoesNotExistThrowsNoSuchElementException() {
        CompositionDTO request = compositionRequest();
        when(compositionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> compositionService.updateComposition(1L, 10L, request));

        verifyNoInteractions(languageDetectorService, kafkaProducerService);
    }

    @Test
    void updateComposition_whenUpdatedLanguageIsUnsupportedThrowsIllegalArgumentException() {
        Composition composition = persistedComposition();
        CompositionDTO request = compositionRequest();
        when(compositionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(composition));
        when(languageDetectorService.detectLanguage("This is my composition.")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> compositionService.updateComposition(1L, 10L, request));

        verify(compositionRepository, never()).save(any(Composition.class));
        verifyNoInteractions(kafkaProducerService);
    }

    @Test
    void deleteComposition_whenCompositionExistsDeletesComposition() {
        Composition composition = persistedComposition();
        when(compositionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(composition));

        compositionService.deleteComposition(1L, 10L);

        verify(compositionRepository).delete(composition);
    }

    @Test
    void deleteComposition_whenCompositionDoesNotExistThrowsNoSuchElementException() {
        when(compositionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> compositionService.deleteComposition(1L, 10L));

        verify(compositionRepository, never()).delete(any(Composition.class));
    }

    @Test
    void requestToGradeComposition_whenCompositionIsDraftMarksReviewingAndSendsEvent() {
        Composition composition = persistedComposition();
        when(compositionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(composition));
        when(compositionRepository.save(composition)).thenReturn(composition);

        CompositionDTO response = compositionService.requestToGradeComposition(1L, 10L);

        ArgumentCaptor<CompositionGradingRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(CompositionGradingRequestedEvent.class);
        verify(kafkaProducerService).sendCompositionGradingRequestedEvent(eventCaptor.capture());
        CompositionGradingRequestedEvent event = eventCaptor.getValue();

        assertEquals(CompositionStatus.REVIEWING, response.getCompositionStatus());
        assertEquals(10L, event.getCompositionId());
        assertEquals(1L, event.getUserId());
        assertEquals("My title", event.getTitle());
        assertEquals("This is my composition.", event.getContent());
        assertEquals(Language.ENGLISH, event.getLanguage());
        assertNotNull(event.getRequestedAt());
    }

    @Test
    void requestToGradeComposition_whenCompositionIsNotDraftThrowsIllegalArgumentException() {
        Composition composition = persistedComposition();
        composition.setCompositionStatus(CompositionStatus.REVIEWED);
        when(compositionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(composition));

        assertThrows(IllegalArgumentException.class,
                () -> compositionService.requestToGradeComposition(1L, 10L));

        verify(compositionRepository, never()).save(any(Composition.class));
        verifyNoInteractions(kafkaProducerService);
    }

    @Test
    void completeCompositionGrading_whenEventIsNullThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> compositionService.completeCompositionGrading(null));

        verifyNoInteractions(compositionRepository);
    }

    @Test
    void completeCompositionGrading_whenUsernameIsNullThrowsIllegalArgumentException() {
        CompositionGradingCompletedEvent event = CompositionGradingCompletedEvent.builder()
                .compositionId(10L)
                .userId(null)
                .success(true)
                .feedback("Good structure")
                .build();

        assertThrows(IllegalArgumentException.class, () -> compositionService.completeCompositionGrading(event));

        verifyNoInteractions(compositionRepository);
    }

    @Test
    void completeCompositionGrading_whenCompositionIdIsNullThrowsIllegalArgumentException() {
        CompositionGradingCompletedEvent event = CompositionGradingCompletedEvent.builder()
                .compositionId(null)
                .userId(1L)
                .success(true)
                .feedback("Good structure")
                .build();

        assertThrows(IllegalArgumentException.class, () -> compositionService.completeCompositionGrading(event));

        verifyNoInteractions(compositionRepository);
    }

    @Test
    void completeCompositionGrading_whenSuccessfulMarksReviewedAndStoresFeedback() {
        Composition composition = persistedComposition();
        LocalDateTime completedAt = LocalDateTime.of(2026, 6, 25, 10, 0);
        CompositionGradingCompletedEvent event = CompositionGradingCompletedEvent.builder()
                .compositionId(10L)
                .userId(1L)
                .success(true)
                .feedback("Good structure")
                .completedAt(completedAt)
                .build();
        when(compositionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(composition));

        compositionService.completeCompositionGrading(event);

        assertEquals(CompositionStatus.REVIEWED, composition.getCompositionStatus());
        assertEquals("Good structure", composition.getSystemFeedback());
        assertEquals(completedAt, composition.getUpdatedAt());
        verify(compositionRepository).save(composition);
    }

    @Test
    void completeCompositionGrading_whenFailedReturnsCompositionToDraftAndStoresFailureReason() {
        Composition composition = persistedComposition();
        composition.setCompositionStatus(CompositionStatus.REVIEWING);
        CompositionGradingCompletedEvent event = CompositionGradingCompletedEvent.builder()
                .compositionId(10L)
                .userId(1L)
                .success(false)
                .failureReason("Evaluation failed")
                .build();
        when(compositionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(composition));

        compositionService.completeCompositionGrading(event);

        assertEquals(CompositionStatus.DRAFT, composition.getCompositionStatus());
        assertEquals("Evaluation failed", composition.getSystemFeedback());
        assertNotNull(composition.getUpdatedAt());
        verify(compositionRepository).save(composition);
    }

    @Test
    void completeCompositionGrading_whenSuccessfulEventHasNullFeedbackThrowsIllegalArgumentException() {
        CompositionGradingCompletedEvent event = CompositionGradingCompletedEvent.builder()
                .compositionId(10L)
                .userId(1L)
                .success(true)
                .feedback(null)
                .build();

        assertThrows(IllegalArgumentException.class, () -> compositionService.completeCompositionGrading(event));

        verifyNoInteractions(compositionRepository);
    }

    @Test
    void completeCompositionGrading_whenSuccessfulEventHasBlankFeedbackThrowsIllegalArgumentException() {
        CompositionGradingCompletedEvent event = CompositionGradingCompletedEvent.builder()
                .compositionId(10L)
                .userId(1L)
                .success(true)
                .feedback(" ")
                .build();

        assertThrows(IllegalArgumentException.class, () -> compositionService.completeCompositionGrading(event));

        verifyNoInteractions(compositionRepository);
    }

    @Test
    void completeCompositionGrading_whenFailedEventHasNullFailureReasonThrowsIllegalArgumentException() {
        CompositionGradingCompletedEvent event = CompositionGradingCompletedEvent.builder()
                .compositionId(10L)
                .userId(1L)
                .success(false)
                .failureReason(null)
                .build();

        assertThrows(IllegalArgumentException.class, () -> compositionService.completeCompositionGrading(event));

        verifyNoInteractions(compositionRepository);
    }

    @Test
    void completeCompositionGrading_whenFailedEventHasBlankFailureReasonThrowsIllegalArgumentException() {
        CompositionGradingCompletedEvent event = CompositionGradingCompletedEvent.builder()
                .compositionId(10L)
                .userId(1L)
                .success(false)
                .failureReason(" ")
                .build();

        assertThrows(IllegalArgumentException.class, () -> compositionService.completeCompositionGrading(event));

        verifyNoInteractions(compositionRepository);
    }

    @Test
    void generateComposition_whenLanguageIsDetectedSavesGeneratingSystemCompositionAndSendsEvent() {
        CompositionDTO request = CompositionDTO.builder()
                .title("Chinese prompt")
                .build();
        when(languageDetectorService.detectLanguage("Chinese prompt")).thenReturn(Optional.of(Language.CHINESE));
        when(compositionRepository.save(any(Composition.class))).thenAnswer(invocation -> {
            Composition composition = invocation.getArgument(0);
            composition.setId(20L);
            return composition;
        });

        CompositionDTO response = compositionService.generateComposition(1L, request);

        ArgumentCaptor<CompositionGenerationRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(CompositionGenerationRequestedEvent.class);
        verify(kafkaProducerService).sendCompositionGenerationRequestedEvent(eventCaptor.capture());
        CompositionGenerationRequestedEvent event = eventCaptor.getValue();

        assertEquals(20L, response.getId());
        assertEquals(1L, response.getUserId());
        assertEquals("Chinese prompt", response.getTitle());
        assertEquals(CompositionSource.SYSTEM, response.getCompositionSource());
        assertEquals(Language.CHINESE, response.getLanguage());
        assertEquals(CompositionStatus.GENERATING, response.getCompositionStatus());
        assertEquals(20L, event.getCompositionId());
        assertEquals(Language.CHINESE, event.getLanguage());
        assertNotNull(event.getRequestedAt());
    }

    @Test
    void generateComposition_whenLanguageCannotBeDetectedDefaultsToEnglish() {
        CompositionDTO request = CompositionDTO.builder()
                .title("12345")
                .build();
        when(languageDetectorService.detectLanguage("12345")).thenReturn(Optional.empty());
        when(compositionRepository.save(any(Composition.class))).thenAnswer(invocation -> {
            Composition composition = invocation.getArgument(0);
            composition.setId(20L);
            return composition;
        });

        CompositionDTO response = compositionService.generateComposition(1L, request);

        ArgumentCaptor<CompositionGenerationRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(CompositionGenerationRequestedEvent.class);
        verify(kafkaProducerService).sendCompositionGenerationRequestedEvent(eventCaptor.capture());
        assertEquals(Language.ENGLISH, response.getLanguage());
        assertEquals(Language.ENGLISH, eventCaptor.getValue().getLanguage());
    }

    @Test
    void generateComposition_whenRequestBodyIsNullThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> compositionService.generateComposition(1L, null));

        verifyNoInteractions(languageDetectorService, compositionRepository, kafkaProducerService);
    }

    @Test
    void generateComposition_whenTitleIsNullThrowsIllegalArgumentException() {
        CompositionDTO request = CompositionDTO.builder()
                .title(null)
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> compositionService.generateComposition(1L, request));

        verifyNoInteractions(languageDetectorService, compositionRepository, kafkaProducerService);
    }

    @Test
    void generateComposition_whenTitleIsBlankThrowsIllegalArgumentException() {
        CompositionDTO request = CompositionDTO.builder()
                .title(" ")
                .build();

        assertThrows(IllegalArgumentException.class,
                () -> compositionService.generateComposition(1L, request));

        verifyNoInteractions(languageDetectorService, compositionRepository, kafkaProducerService);
    }

    @Test
    void completeCompositionGeneration_whenSuccessfulStoresGeneratedContentAndMarksDraft() {
        Composition composition = persistedComposition();
        composition.setCompositionStatus(CompositionStatus.GENERATING);
        composition.setSystemFeedback("previous feedback");
        LocalDateTime completedAt = LocalDateTime.of(2026, 6, 25, 11, 0);
        CompositionGenerationCompletedEvent event = CompositionGenerationCompletedEvent.builder()
                .compositionId(10L)
                .userId(1L)
                .success(true)
                .generatedContent("Generated composition")
                .completedAt(completedAt)
                .build();
        when(compositionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(composition));

        compositionService.completeCompositionGeneration(event);

        assertEquals("Generated composition", composition.getContent());
        assertEquals(CompositionStatus.DRAFT, composition.getCompositionStatus());
        assertNull(composition.getSystemFeedback());
        assertEquals(completedAt, composition.getUpdatedAt());
        verify(compositionRepository).save(composition);
    }

    @Test
    void completeCompositionGeneration_whenFailedMarksGenerationFailedAndStoresFailureReason() {
        Composition composition = persistedComposition();
        composition.setCompositionStatus(CompositionStatus.GENERATING);
        CompositionGenerationCompletedEvent event = CompositionGenerationCompletedEvent.builder()
                .compositionId(10L)
                .userId(1L)
                .success(false)
                .failureReason("Generation failed")
                .build();
        when(compositionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(composition));

        compositionService.completeCompositionGeneration(event);

        assertEquals(CompositionStatus.GENERATION_FAILED, composition.getCompositionStatus());
        assertEquals("Generation failed", composition.getSystemFeedback());
        assertNotNull(composition.getUpdatedAt());
        verify(compositionRepository).save(composition);
    }

    @Test
    void completeCompositionGeneration_whenEventIsNullThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> compositionService.completeCompositionGeneration(null));

        verifyNoInteractions(compositionRepository);
    }

    @Test
    void completeCompositionGeneration_whenUsernameIsNullThrowsIllegalArgumentException() {
        CompositionGenerationCompletedEvent event = CompositionGenerationCompletedEvent.builder()
                .compositionId(10L)
                .userId(null)
                .success(true)
                .generatedContent("Generated composition")
                .build();

        assertThrows(IllegalArgumentException.class, () -> compositionService.completeCompositionGeneration(event));

        verifyNoInteractions(compositionRepository);
    }

    @Test
    void completeCompositionGeneration_whenCompositionIdIsNullThrowsIllegalArgumentException() {
        CompositionGenerationCompletedEvent event = CompositionGenerationCompletedEvent.builder()
                .compositionId(null)
                .userId(1L)
                .success(true)
                .generatedContent("Generated composition")
                .build();

        assertThrows(IllegalArgumentException.class, () -> compositionService.completeCompositionGeneration(event));

        verifyNoInteractions(compositionRepository);
    }

    @Test
    void completeCompositionGeneration_whenSuccessfulEventHasNullGeneratedContentThrowsIllegalArgumentException() {
        CompositionGenerationCompletedEvent event = CompositionGenerationCompletedEvent.builder()
                .compositionId(10L)
                .userId(1L)
                .success(true)
                .generatedContent(null)
                .build();

        assertThrows(IllegalArgumentException.class, () -> compositionService.completeCompositionGeneration(event));

        verifyNoInteractions(compositionRepository);
    }

    @Test
    void completeCompositionGeneration_whenSuccessfulEventHasBlankGeneratedContentThrowsIllegalArgumentException() {
        CompositionGenerationCompletedEvent event = CompositionGenerationCompletedEvent.builder()
                .compositionId(10L)
                .userId(1L)
                .success(true)
                .generatedContent(" ")
                .build();

        assertThrows(IllegalArgumentException.class, () -> compositionService.completeCompositionGeneration(event));

        verifyNoInteractions(compositionRepository);
    }

    @Test
    void completeCompositionGeneration_whenFailedEventHasNullFailureReasonThrowsIllegalArgumentException() {
        CompositionGenerationCompletedEvent event = CompositionGenerationCompletedEvent.builder()
                .compositionId(10L)
                .userId(1L)
                .success(false)
                .failureReason(null)
                .build();

        assertThrows(IllegalArgumentException.class, () -> compositionService.completeCompositionGeneration(event));

        verifyNoInteractions(compositionRepository);
    }

    @Test
    void completeCompositionGeneration_whenFailedEventHasBlankFailureReasonThrowsIllegalArgumentException() {
        CompositionGenerationCompletedEvent event = CompositionGenerationCompletedEvent.builder()
                .compositionId(10L)
                .userId(1L)
                .success(false)
                .failureReason(" ")
                .build();

        assertThrows(IllegalArgumentException.class, () -> compositionService.completeCompositionGeneration(event));

        verifyNoInteractions(compositionRepository);
    }

    @Test
    void completeCompositionGeneration_whenCompositionDoesNotExistThrowsNoSuchElementException() {
        CompositionGenerationCompletedEvent event = CompositionGenerationCompletedEvent.builder()
                .compositionId(10L)
                .userId(1L)
                .success(true)
                .generatedContent("Generated composition")
                .build();
        when(compositionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> compositionService.completeCompositionGeneration(event));

        verify(compositionRepository, never()).save(any(Composition.class));
    }

    private static CompositionDTO compositionRequest() {
        return CompositionDTO.builder()
                .title("My title")
                .content("This is my composition.")
                .build();
    }

    private static Composition persistedComposition() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 25, 9, 0);
        return Composition.builder()
                .id(10L)
                .userId(1L)
                .title("My title")
                .content("This is my composition.")
                .compositionSource(CompositionSource.RAW_TEXT)
                .language(Language.ENGLISH)
                .compositionStatus(CompositionStatus.DRAFT)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }
}
