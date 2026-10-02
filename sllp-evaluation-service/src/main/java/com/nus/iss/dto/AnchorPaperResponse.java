package com.nus.iss.dto;

import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.Language;

import java.util.List;

public record AnchorPaperResponse(
        Long anchorId,
        String title,
        String composition,
        Language language,
        DifficultyLevel difficultyLevel,
        List<AnchorScoreResponse> scores) {
}
