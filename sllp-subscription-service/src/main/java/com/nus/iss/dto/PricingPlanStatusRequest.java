package com.nus.iss.dto;
import jakarta.validation.constraints.NotNull;
public record PricingPlanStatusRequest(@NotNull Boolean active) {}
