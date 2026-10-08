package com.nus.iss.dto;

import com.nus.iss.enums.ChallengeCadence;
import jakarta.validation.constraints.*;

public record ChallengeTemplateRequest(
        @NotBlank @Size(max = 100) String code,
        @NotNull ChallengeCadence cadence,
        @NotBlank @Pattern(regexp = "ENGLISH|CHINESE", message = "Language must be ENGLISH or CHINESE") String language,
        @Size(max = 32) String category,
        @Size(max = 64) String themePath,
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 5000) String prompt,
        @Min(1) int minResponseUnits,
        @Min(1) int maxResponseUnits,
        boolean active,
        @Min(0) int displayOrder) { }
