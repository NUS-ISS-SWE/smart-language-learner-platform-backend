package com.nus.iss.repository;

import com.nus.iss.entity.AgentReviewQueueItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentReviewQueueRepository extends JpaRepository<AgentReviewQueueItem, UUID> {
    boolean existsByRequestId(UUID requestId);
    List<AgentReviewQueueItem> findTop100ByStatusOrderByCreatedAtDesc(String status);
}
