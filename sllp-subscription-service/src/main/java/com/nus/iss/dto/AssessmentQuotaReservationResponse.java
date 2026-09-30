package com.nus.iss.dto;

import java.time.LocalDate;
import java.util.UUID;

public record AssessmentQuotaReservationResponse(UUID requestId, String planCode, int monthlyAssessmentLimit,
                                                  int assessmentsUsed, LocalDate renewsAt) {}
