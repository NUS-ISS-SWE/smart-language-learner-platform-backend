package com.nus.iss.dto;

import jakarta.validation.constraints.*;

public record AchievementDefinitionRequest(
        @NotBlank @Size(max = 100) String code,
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 500) String description,
        @NotBlank @Pattern(regexp = "[A-Z_]+", message = "Category must use uppercase underscore notation") String category,
        @Min(1) int threshold,
        boolean active) { }
