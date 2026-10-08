package com.nus.iss.service.impl;

import com.nus.iss.dto.AgentReviewQueueResponse;
import com.nus.iss.entity.AgentReviewQueueItem;
import com.nus.iss.repository.AgentReviewQueueRepository;
import com.nus.iss.service.AgentReviewQueueService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor
public class AgentReviewQueueServiceImpl implements AgentReviewQueueService {
    private final AgentReviewQueueRepository repository;

    @Override
    public void recordFailure(UUID requestId, Long compositionId, Long userId, String reason) {
        if (repository.existsByRequestId(requestId)) return;
        repository.save(AgentReviewQueueItem.builder().requestId(requestId).compositionId(compositionId).userId(userId)
                .failureReason(reason == null ? "Assessment workflow failed" : reason.substring(0, Math.min(reason.length(), 500)))
                .createdAt(LocalDateTime.now()).build());
    }

    @Override
    public List<AgentReviewQueueResponse> openItems() {
        return repository.findTop100ByStatusOrderByCreatedAtDesc("OPEN").stream()
                .map(item -> new AgentReviewQueueResponse(item.getId(), item.getRequestId(), item.getCompositionId(), item.getUserId(),
                        item.getStatus(), item.getFailureReason(), item.getCreatedAt()))
                .toList();
    }
}
