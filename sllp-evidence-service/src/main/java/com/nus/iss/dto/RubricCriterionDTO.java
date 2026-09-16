package com.nus.iss.dto;

import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.Language;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RubricCriterionDTO(
        Long id,
        Language language,
        DifficultyLevel difficultyLevel,
        String criterionName,
        String criterionDescription,
        BigDecimal weightPercentage,
        String createdBy,
        String updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
