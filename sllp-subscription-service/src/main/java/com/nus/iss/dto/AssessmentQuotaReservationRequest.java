package com.nus.iss.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record AssessmentQuotaReservationRequest(@NotNull @Positive Long userId, @NotNull UUID requestId) {}
