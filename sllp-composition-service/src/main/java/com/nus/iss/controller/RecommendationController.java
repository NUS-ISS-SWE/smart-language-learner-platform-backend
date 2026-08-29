package com.nus.iss.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nus.iss.dto.RecommendationResponse;
import com.nus.iss.service.RecommendationService;

/** HTTP adapter for composition learning recommendations. */
@RestController
@RequestMapping("/api/v1")
public class RecommendationController {
    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping("/recommendations/generate")
    public ResponseEntity<List<RecommendationResponse>> generateRecommendations(
            @RequestHeader("X-Sllp-User-Id") Long userId) {
        return ResponseEntity.ok(recommendationService.generateRecommendations(userId));
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<RecommendationResponse>> getRecommendations(
            @RequestHeader("X-Sllp-User-Id") Long userId) {
        return ResponseEntity.ok(recommendationService.getRecommendations(userId));
    }
}
