package com.nus.iss.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nus.iss.dto.RecommendationResponse;
import com.nus.iss.entity.Evaluation;
import com.nus.iss.entity.Recommendation;
import com.nus.iss.repository.EvaluationRepository;
import com.nus.iss.repository.RecommendationRepository;
import com.nus.iss.service.CompositionAccessService;
import com.nus.iss.service.RecommendationService;

import lombok.extern.slf4j.Slf4j;

/** Analyzes user performance and generates personalized learning recommendations. */
@Slf4j
@Service
public class RecommendationServiceImpl implements RecommendationService {
    private final EvaluationRepository evaluationRepository;
    private final RecommendationRepository recommendationRepository;
    private final CompositionAccessService compositionAccessService;

    public RecommendationServiceImpl(EvaluationRepository evaluationRepository,
                                    RecommendationRepository recommendationRepository,
                                    CompositionAccessService compositionAccessService) {
        this.evaluationRepository = evaluationRepository;
        this.recommendationRepository = recommendationRepository;
        this.compositionAccessService = compositionAccessService;
    }

    @Override
    @Transactional
    public List<RecommendationResponse> generateRecommendations(Long userId) {
        compositionAccessService.requireUserId(userId);
        List<Evaluation> evaluations = evaluationRepository.findByUserIdOrderByEvaluatedAtDesc(userId);

        if (evaluations.isEmpty()) {
            log.info("No evaluations found for user: {}", userId);
            return List.of();
        }

        BigDecimal averageScore = calculateAverageScore(evaluations);
        List<Recommendation> recommendations = analyzePerformance(userId, evaluations, averageScore);

        // Clear previous recommendations for this user
        List<Recommendation> existingRecommendations = recommendationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        recommendationRepository.deleteAll(existingRecommendations);

        // Save new recommendations
        List<Recommendation> saved = recommendationRepository.saveAll(recommendations);
        return mapToResponses(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationResponse> getRecommendations(Long userId) {
        compositionAccessService.requireUserId(userId);
        List<Recommendation> recommendations = recommendationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return mapToResponses(recommendations);
    }

    private BigDecimal calculateAverageScore(List<Evaluation> evaluations) {
        return evaluations.stream()
                .filter(Evaluation::isSuccess)
                .map(Evaluation::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(1, evaluations.size())), BigDecimal.ROUND_HALF_UP);
    }

    private List<Recommendation> analyzePerformance(Long userId, List<Evaluation> evaluations, BigDecimal averageScore) {
        List<Recommendation> recommendations = new ArrayList<>();
        String combinedFeedback = evaluations.stream()
                .filter(Evaluation::isSuccess)
                .map(Evaluation::getFeedback)
                .collect(Collectors.joining(" "));

        // Analyze for grammar issues
        if (containsPattern(combinedFeedback, "grammar|tense|subject-verb|agreement|punctuation")) {
            recommendations.add(Recommendation.builder()
                    .userId(userId)
                    .recommendationType("GRAMMAR")
                    .recommendation("Focus on improving grammar and sentence structure. Review verb tenses and subject-verb agreement.")
                    .performanceScore(averageScore)
                    .reasoning("Your feedback frequently mentions grammar issues. Master this foundation to improve overall writing quality.")
                    .build());
        }

        // Analyze for vocabulary issues
        if (containsPattern(combinedFeedback, "vocabulary|word choice|expression|adjective|adverb")) {
            recommendations.add(Recommendation.builder()
                    .userId(userId)
                    .recommendationType("VOCABULARY")
                    .recommendation("Expand your vocabulary with contextual learning. Practice using synonyms and varied expressions.")
                    .performanceScore(averageScore)
                    .reasoning("Feedback suggests expanding vocabulary range. More varied word choices will make writing more engaging.")
                    .build());
        }

        // Analyze for structure/organization issues
        if (containsPattern(combinedFeedback, "structure|organization|flow|coherence|transition|paragraph")) {
            recommendations.add(Recommendation.builder()
                    .userId(userId)
                    .recommendationType("STRUCTURE")
                    .recommendation("Improve composition structure with better transitions and paragraph organization.")
                    .performanceScore(averageScore)
                    .reasoning("Your compositions need better organization. Work on logical flow and clear transitions between ideas.")
                    .build());
        }

        // Analyze for complexity/depth
        if (averageScore.compareTo(BigDecimal.valueOf(70)) < 0) {
            recommendations.add(Recommendation.builder()
                    .userId(userId)
                    .recommendationType("COMPLEXITY")
                    .recommendation("Practice writing more complex sentences. Use compound and complex sentence structures to demonstrate advanced skills.")
                    .performanceScore(averageScore)
                    .reasoning("Your current score indicates room for improvement in writing complexity. Challenge yourself with more sophisticated structures.")
                    .build());
        }

        // High performer - advanced recommendations
        if (averageScore.compareTo(BigDecimal.valueOf(85)) >= 0) {
            recommendations.add(Recommendation.builder()
                    .userId(userId)
                    .recommendationType("ADVANCEMENT")
                    .recommendation("You're performing well! Try writing on advanced topics or in different genres to expand your skills.")
                    .performanceScore(averageScore)
                    .reasoning("Your strong performance suggests you're ready for more challenging compositions and diverse writing styles.")
                    .build());
        }

        log.info("Generated {} recommendations for user: {} with average score: {}", recommendations.size(), userId, averageScore);
        return recommendations;
    }

    private boolean containsPattern(String text, String pattern) {
        return text != null && text.toLowerCase().matches(".*(" + pattern + ").*");
    }

    private List<RecommendationResponse> mapToResponses(List<Recommendation> recommendations) {
        return recommendations.stream()
                .map(r -> new RecommendationResponse(r.getId(), r.getRecommendationType(), r.getRecommendation(),
                        r.getPerformanceScore(), r.getReasoning()))
                .collect(Collectors.toList());
    }
}
