package com.nus.iss.dto;
import java.util.List;
import jakarta.validation.constraints.*;
public record PricingPlanRequest(@Pattern(regexp = "[A-Z][A-Z0-9_]{1,49}") String code, @NotBlank @Size(max = 120) String displayName, @Size(max = 4000) String description, @NotNull @PositiveOrZero Long priceMinor, @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency, @Positive Integer durationDays, @NotNull List<@NotBlank @Size(max = 500) String> features, @NotNull @Min(0) Integer displayOrder, @NotNull @Min(1) Integer monthlyAssessmentLimit) {
    public PricingPlanRequest(String code, String displayName, String description, Long priceMinor, String currency, Integer durationDays, List<String> features, Integer displayOrder) {
        this(code, displayName, description, priceMinor, currency, durationDays, features, displayOrder, 20);
    }
}
