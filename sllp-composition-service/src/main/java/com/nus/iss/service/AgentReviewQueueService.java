package com.nus.iss.service;

import com.nus.iss.dto.AgentReviewQueueResponse;
import java.util.List;
import java.util.UUID;

public interface AgentReviewQueueService {
    void recordFailure(UUID requestId, Long compositionId, Long userId, String reason);
    List<AgentReviewQueueResponse> openItems();

    static AgentReviewQueueService noop() {
        return new AgentReviewQueueService() {
            @Override public void recordFailure(UUID requestId, Long compositionId, Long userId, String reason) {}
            @Override public List<AgentReviewQueueResponse> openItems() { return List.of(); }
        };
    }
}
