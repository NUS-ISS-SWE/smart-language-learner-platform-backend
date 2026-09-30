package com.nus.iss.service;

import com.nus.iss.dto.AssessmentQuotaReservationResponse;
import java.util.UUID;

public interface AssessmentQuotaService {
    AssessmentQuotaReservationResponse reserve(Long userId, UUID requestId);
    void settle(UUID requestId, boolean consumed);
}
