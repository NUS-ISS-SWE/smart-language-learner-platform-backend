package com.nus.iss.dto;

import com.nus.iss.enums.DifficultyLevel;

public record UserProfileDTO(
        Long id,
        String username,
        DifficultyLevel difficultyLevel
) {
}
