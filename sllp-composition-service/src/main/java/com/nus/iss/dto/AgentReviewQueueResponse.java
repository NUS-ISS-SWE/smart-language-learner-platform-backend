package com.nus.iss.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AgentReviewQueueResponse(UUID id, UUID requestId, Long compositionId, Long userId,
                                       String status, String failureReason, LocalDateTime createdAt) {}
