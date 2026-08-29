package com.nus.iss.service;

import java.util.List;

import com.nus.iss.dto.RecommendationResponse;

/** Analyzes user performance and generates learning recommendations. */
public interface RecommendationService {
    /**
     * Generate personalized recommendations for a user based on their performance.
     *
     * @param userId the user ID
     * @return list of recommendations
     */
    List<RecommendationResponse> generateRecommendations(Long userId);

    /**
     * Get previously generated recommendations for a user.
     *
     * @param userId the user ID
     * @return list of recommendations
     */
    List<RecommendationResponse> getRecommendations(Long userId);
}
