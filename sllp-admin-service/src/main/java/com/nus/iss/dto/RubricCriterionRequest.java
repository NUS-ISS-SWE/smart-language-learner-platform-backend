package com.nus.iss.dto;

import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.Language;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RubricCriterionRequest(
        @NotNull Language language,
        @NotNull DifficultyLevel difficultyLevel,
        @NotBlank @Size(max = 200) String criterionName,
        @NotBlank String criterionDescription,
        @NotNull @Digits(integer = 3, fraction = 2)
        @DecimalMin(value = "0.01") @DecimalMax(value = "100.00") BigDecimal weightPercentage
) {
}
