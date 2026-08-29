package com.nus.iss.dto;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
public record PricingPlanOrderRequest(@NotEmpty List<@Valid Item> items) { public record Item(@NotBlank String code, @NotNull @Min(0) Integer displayOrder) {} }
