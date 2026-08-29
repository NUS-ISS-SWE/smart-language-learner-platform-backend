package com.nus.iss.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.nus.iss.dto.RecommendationResponse;
import com.nus.iss.service.RecommendationService;

@ExtendWith(MockitoExtension.class)
class RecommendationControllerTest {
    @Mock private RecommendationService recommendationService;

    @Test
    void generateRecommendationsDelegatesAndReturns200() {
        RecommendationController controller = new RecommendationController(recommendationService);
        List<RecommendationResponse> recommendations = List.of(
                new RecommendationResponse(1L, "GRAMMAR", "Improve grammar", java.math.BigDecimal.valueOf(75), "Test reasoning")
        );
        when(recommendationService.generateRecommendations(1L)).thenReturn(recommendations);

        ResponseEntity<List<RecommendationResponse>> result = controller.generateRecommendations(1L);

        assertEquals(200, result.getStatusCode().value());
        assertSame(recommendations, result.getBody());
        verify(recommendationService).generateRecommendations(1L);
    }

    @Test
    void getRecommendationsDelegatesAndReturns200() {
        RecommendationController controller = new RecommendationController(recommendationService);
        List<RecommendationResponse> recommendations = List.of(
                new RecommendationResponse(1L, "VOCABULARY", "Expand vocabulary", java.math.BigDecimal.valueOf(80), "Test reasoning")
        );
        when(recommendationService.getRecommendations(1L)).thenReturn(recommendations);

        ResponseEntity<List<RecommendationResponse>> result = controller.getRecommendations(1L);

        assertEquals(200, result.getStatusCode().value());
        assertSame(recommendations, result.getBody());
        verify(recommendationService).getRecommendations(1L);
    }
}
