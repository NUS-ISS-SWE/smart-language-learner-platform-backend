package com.nus.iss.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nus.iss.dto.RecommendationResponse;
import com.nus.iss.entity.Evaluation;
import com.nus.iss.entity.Recommendation;
import com.nus.iss.repository.EvaluationRepository;
import com.nus.iss.repository.RecommendationRepository;
import com.nus.iss.service.CompositionAccessService;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceImplTest {
    @Mock private EvaluationRepository evaluationRepository;
    @Mock private RecommendationRepository recommendationRepository;
    @Mock private CompositionAccessService compositionAccessService;

    @Test
    void generateRecommendationsAnalyzesPerformanceAndStoresSuggestions() {
        RecommendationServiceImpl service = new RecommendationServiceImpl(evaluationRepository, recommendationRepository, compositionAccessService);

        Evaluation eval1 = Evaluation.builder().compositionId(1L).score(BigDecimal.valueOf(75))
                .feedback("Good grammar but improve vocabulary").success(true).evaluatedAt(LocalDateTime.now()).build();
        Evaluation eval2 = Evaluation.builder().compositionId(2L).score(BigDecimal.valueOf(80))
                .feedback("Better structure needed for organization").success(true).evaluatedAt(LocalDateTime.now()).build();

        when(evaluationRepository.findByUserIdOrderByEvaluatedAtDesc(1L)).thenReturn(List.of(eval1, eval2));
        when(recommendationRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(recommendationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<RecommendationResponse> results = service.generateRecommendations(1L);

        assertTrue(results.size() > 0, "Should generate recommendations");
        assertTrue(results.stream().anyMatch(r -> "VOCABULARY".equals(r.recommendationType())),
                "Should recommend vocabulary improvement");
        assertTrue(results.stream().anyMatch(r -> "STRUCTURE".equals(r.recommendationType())),
                "Should recommend structure improvement");
        verify(evaluationRepository).findByUserIdOrderByEvaluatedAtDesc(1L);
    }

    @Test
    void generateRecommendationsHandlesNoEvaluations() {
        RecommendationServiceImpl service = new RecommendationServiceImpl(evaluationRepository, recommendationRepository, compositionAccessService);

        when(evaluationRepository.findByUserIdOrderByEvaluatedAtDesc(1L)).thenReturn(List.of());

        List<RecommendationResponse> results = service.generateRecommendations(1L);

        assertTrue(results.isEmpty(), "Should return empty list when no evaluations exist");
    }

    @Test
    void generateRecommendationsSuggestsAdvancementForHighScores() {
        RecommendationServiceImpl service = new RecommendationServiceImpl(evaluationRepository, recommendationRepository, compositionAccessService);

        Evaluation eval = Evaluation.builder().compositionId(1L).score(BigDecimal.valueOf(90))
                .feedback("Excellent work").success(true).evaluatedAt(LocalDateTime.now()).build();

        when(evaluationRepository.findByUserIdOrderByEvaluatedAtDesc(1L)).thenReturn(List.of(eval));
        when(recommendationRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(recommendationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<RecommendationResponse> results = service.generateRecommendations(1L);

        assertTrue(results.stream().anyMatch(r -> "ADVANCEMENT".equals(r.recommendationType())),
                "Should recommend advancement for high scores");
    }

    @Test
    void generateRecommendationsSuggestsComplexityForLowScores() {
        RecommendationServiceImpl service = new RecommendationServiceImpl(evaluationRepository, recommendationRepository, compositionAccessService);

        Evaluation eval = Evaluation.builder().compositionId(1L).score(BigDecimal.valueOf(50))
                .feedback("Needs improvement in all areas").success(true).evaluatedAt(LocalDateTime.now()).build();

        when(evaluationRepository.findByUserIdOrderByEvaluatedAtDesc(1L)).thenReturn(List.of(eval));
        when(recommendationRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(recommendationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<RecommendationResponse> results = service.generateRecommendations(1L);

        assertTrue(results.stream().anyMatch(r -> "COMPLEXITY".equals(r.recommendationType())),
                "Should recommend complexity improvement for low scores");
    }

    @Test
    void getRecommendationsReturnsPreviouslyGenerated() {
        RecommendationServiceImpl service = new RecommendationServiceImpl(evaluationRepository, recommendationRepository, compositionAccessService);

        Recommendation rec = Recommendation.builder().id(1L).userId(1L).recommendationType("GRAMMAR")
                .recommendation("Test recommendation").performanceScore(BigDecimal.valueOf(75))
                .reasoning("Test reasoning").createdAt(LocalDateTime.now()).build();

        when(recommendationRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(rec));

        List<RecommendationResponse> results = service.getRecommendations(1L);

        assertEquals(1, results.size());
        assertEquals("GRAMMAR", results.get(0).recommendationType());
    }

    @Test
    void generateRecommendationsDetectsGrammarIssues() {
        RecommendationServiceImpl service = new RecommendationServiceImpl(evaluationRepository, recommendationRepository, compositionAccessService);

        Evaluation eval = Evaluation.builder().compositionId(1L).score(BigDecimal.valueOf(70))
                .feedback("Subject-verb agreement needs work. Verb tenses are incorrect.").success(true)
                .evaluatedAt(LocalDateTime.now()).build();

        when(evaluationRepository.findByUserIdOrderByEvaluatedAtDesc(1L)).thenReturn(List.of(eval));
        when(recommendationRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(recommendationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<RecommendationResponse> results = service.generateRecommendations(1L);

        assertTrue(results.stream().anyMatch(r -> "GRAMMAR".equals(r.recommendationType())),
                "Should detect and recommend grammar improvements");
    }

    @Test
    void generateRecommendationsClearsOldRecommendations() {
        RecommendationServiceImpl service = new RecommendationServiceImpl(evaluationRepository, recommendationRepository, compositionAccessService);

        Recommendation oldRec = Recommendation.builder().id(1L).userId(1L).recommendationType("OLD")
                .recommendation("Old recommendation").performanceScore(BigDecimal.valueOf(60))
                .reasoning("Old reasoning").createdAt(LocalDateTime.now()).build();

        Evaluation newEval = Evaluation.builder().compositionId(1L).score(BigDecimal.valueOf(90))
                .feedback("Excellent").success(true).evaluatedAt(LocalDateTime.now()).build();

        when(evaluationRepository.findByUserIdOrderByEvaluatedAtDesc(1L)).thenReturn(List.of(newEval));
        when(recommendationRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(oldRec));
        when(recommendationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.generateRecommendations(1L);

        verify(recommendationRepository).deleteAll(List.of(oldRec));
    }
}
