package com.nus.iss.dto;

import java.time.LocalDateTime;

public record AdminUserResponseDTO(
        Long id,
        String username,
        String email,
        String role,
        String status,
        String preferredLanguage,
        String difficultyLevel,
        Long aiTokenBalance,
        Long aiTokenUsed,
        LocalDateTime createdAt,
        LocalDateTime deletedAt
) {
}
