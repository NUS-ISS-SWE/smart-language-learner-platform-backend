package com.nus.iss.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nus.iss.dto.CompositionGenerationCompletedEvent;
import com.nus.iss.dto.CompositionGradingCompletedEvent;
import com.nus.iss.dto.CompositionGradingProgressEvent;
import com.nus.iss.entity.Composition;
import com.nus.iss.enums.CompositionStatus;
import com.nus.iss.repository.AiUsageEventRepository;
import com.nus.iss.repository.CompositionRepository;
import com.nus.iss.repository.EvaluationRepository;

@ExtendWith(MockitoExtension.class)
class CompositionCompletionServiceTest {
    @Mock private CompositionRepository compositionRepository;
    @Mock private EvaluationRepository evaluationRepository;
    @Mock private AiUsageEventRepository aiUsageEventRepository;

    private CompositionCompletionService completionService;

    @BeforeEach
    void setUp() {
        completionService = new CompositionCompletionService(compositionRepository, evaluationRepository, aiUsageEventRepository,
                new CompositionAccessService(compositionRepository));
    }

    @Test
    void completeGradingMarksSuccessfulCompositionReviewedAndStoresEvaluation() {
        Composition composition = composition();
        LocalDateTime completedAt = LocalDateTime.of(2026, 6, 25, 10, 0);
        when(compositionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(composition));
        stubEvaluationInsert(1);

        completionService.completeGrading(CompositionGradingCompletedEvent.builder()
                .compositionId(10L).userId(1L).success(true).score(BigDecimal.valueOf(90)).feedback("Good work")
                .completedAt(completedAt).build());

        assertEquals(CompositionStatus.REVIEWED, composition.getCompositionStatus());
        assertEquals("Good work", composition.getSystemFeedback());
        assertNull(composition.getGradingProgressMessage());
        assertEquals(completedAt, composition.getUpdatedAt());
        verify(compositionRepository).save(composition);
        verify(evaluationRepository).insertIfAbsent(anyLong(), any(), any(), any(), any(), anyBoolean(), any(), any());
    }

    @Test
    void recordGradingProgressStoresStudentSafeMessageOnlyWhileReviewing() {
        Composition composition = composition();
        composition.setCompositionStatus(CompositionStatus.REVIEWING);
        when(compositionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(composition));

        completionService.recordGradingProgress(CompositionGradingProgressEvent.builder()
                .compositionId(10L).userId(1L).stage("GRADING")
                .message("Assessing your composition…").build());

        assertEquals("Assessing your composition…", composition.getGradingProgressMessage());
        verify(compositionRepository).save(composition);
    }

    @Test
    void recordGradingProgressIgnoresDelayedEventsAfterCompletion() {
        Composition composition = composition();
        composition.setCompositionStatus(CompositionStatus.REVIEWED);
        when(compositionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(composition));

        completionService.recordGradingProgress(CompositionGradingProgressEvent.builder()
                .compositionId(10L).userId(1L).message("Assessing your composition…").build());

        verify(compositionRepository, never()).save(any());
    }

    @Test
    void completeGradingReturnsFailedCompositionToDraft() {
        Composition composition = composition();
        composition.setCompositionStatus(CompositionStatus.REVIEWING);
        when(compositionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(composition));
        stubEvaluationInsert(1);

        completionService.completeGrading(CompositionGradingCompletedEvent.builder()
                .compositionId(10L).userId(1L).success(false).failureReason("Evaluator unavailable").build());

        assertEquals(CompositionStatus.DRAFT, composition.getCompositionStatus());
        assertEquals("Evaluator unavailable", composition.getSystemFeedback());
        assertNotNull(composition.getUpdatedAt());
        verify(evaluationRepository).insertIfAbsent(anyLong(), any(), any(), any(), any(), anyBoolean(), any(), any());
    }

    @Test
    void completeGradingIgnoresAnAlreadyRecordedRequest() {
        Composition composition = composition();
        java.util.UUID requestId = java.util.UUID.randomUUID();
        when(compositionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(composition));
        stubEvaluationInsert(0);

        completionService.completeGrading(CompositionGradingCompletedEvent.builder()
                .requestId(requestId).compositionId(10L).userId(1L).success(true).feedback("Good work").build());

        verify(compositionRepository, never()).save(any());
        verify(aiUsageEventRepository, never()).save(any());
    }

    @Test
    void completeGradingPersistsUsageOnlyWithTheNewEvaluation() {
        Composition composition = composition();
        java.util.UUID requestId = java.util.UUID.randomUUID();
        when(compositionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(composition));
        stubEvaluationInsert(1);

        completionService.completeGrading(CompositionGradingCompletedEvent.builder()
                .requestId(requestId).compositionId(10L).userId(1L).success(true).feedback("Good work")
                .totalTokens(12L).promptTokens(8L).completionTokens(4L).cachedInputTokens(0L)
                .estimatedCostMicros(7L).aiProvider("OPENAI").aiModel("gpt-4o-mini")
                .pricingVersion("test").inputPriceMicrosPerMillion(1L)
                .cachedInputPriceMicrosPerMillion(1L).outputPriceMicrosPerMillion(1L).build());

        verify(aiUsageEventRepository).save(any());
    }

    @Test
    void completeGradingRejectsInvalidEventsAndMissingComposition() {
        assertThrows(IllegalArgumentException.class, () -> completionService.completeGrading(null));
        assertThrows(IllegalArgumentException.class, () -> completionService.completeGrading(
                CompositionGradingCompletedEvent.builder().compositionId(10L).userId(null).success(true).feedback("Good").build()));
        assertThrows(IllegalArgumentException.class, () -> completionService.completeGrading(
                CompositionGradingCompletedEvent.builder().compositionId(10L).userId(1L).success(true).feedback(" ").build()));
        assertThrows(IllegalArgumentException.class, () -> completionService.completeGrading(
                CompositionGradingCompletedEvent.builder().compositionId(10L).userId(1L).success(false).failureReason(" ").build()));

        when(compositionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> completionService.completeGrading(
                CompositionGradingCompletedEvent.builder().compositionId(10L).userId(1L).success(true).feedback("Good").build()));
        verify(compositionRepository, never()).save(any());
    }

    @Test
    void completeGenerationStoresSuccessfulContentAndClearsFeedback() {
        Composition composition = composition();
        composition.setCompositionStatus(CompositionStatus.GENERATING);
        composition.setSystemFeedback("Old feedback");
        LocalDateTime completedAt = LocalDateTime.of(2026, 6, 25, 11, 0);
        when(compositionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(composition));

        completionService.completeGeneration(CompositionGenerationCompletedEvent.builder()
                .compositionId(10L).userId(1L).success(true).generatedContent("Generated composition")
                .completedAt(completedAt).build());

        assertEquals("Generated composition", composition.getContent());
        assertEquals(CompositionStatus.DRAFT, composition.getCompositionStatus());
        assertNull(composition.getSystemFeedback());
        assertEquals(completedAt, composition.getUpdatedAt());
        verify(compositionRepository).save(composition);
    }

    @Test
    void completeGenerationMarksFailureAndRejectsInvalidEvents() {
        Composition composition = composition();
        composition.setCompositionStatus(CompositionStatus.GENERATING);
        when(compositionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(composition));

        completionService.completeGeneration(CompositionGenerationCompletedEvent.builder()
                .compositionId(10L).userId(1L).success(false).failureReason("Generation failed").build());
        assertEquals(CompositionStatus.GENERATION_FAILED, composition.getCompositionStatus());
        assertEquals("Generation failed", composition.getSystemFeedback());

        assertThrows(IllegalArgumentException.class, () -> completionService.completeGeneration(null));
        assertThrows(IllegalArgumentException.class, () -> completionService.completeGeneration(
                CompositionGenerationCompletedEvent.builder().compositionId(null).userId(1L).success(true).generatedContent("Text").build()));
        assertThrows(IllegalArgumentException.class, () -> completionService.completeGeneration(
                CompositionGenerationCompletedEvent.builder().compositionId(10L).userId(1L).success(true).generatedContent(" ").build()));
        assertThrows(IllegalArgumentException.class, () -> completionService.completeGeneration(
                CompositionGenerationCompletedEvent.builder().compositionId(10L).userId(1L).success(false).failureReason(" ").build()));
    }

    @Test
    void completeGenerationDoesNotSaveMissingComposition() {
        when(compositionRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> completionService.completeGeneration(
                CompositionGenerationCompletedEvent.builder().compositionId(10L).userId(1L).success(true).generatedContent("Text").build()));

        verify(compositionRepository, never()).save(any());
    }

    private static Composition composition() {
        return Composition.builder().id(10L).userId(1L).title("Title").content("Original")
                .compositionStatus(CompositionStatus.DRAFT).build();
    }

    private void stubEvaluationInsert(int result) {
        when(evaluationRepository.insertIfAbsent(anyLong(), any(), any(), any(), any(), anyBoolean(), any(), any()))
                .thenReturn(result);
    }
}
