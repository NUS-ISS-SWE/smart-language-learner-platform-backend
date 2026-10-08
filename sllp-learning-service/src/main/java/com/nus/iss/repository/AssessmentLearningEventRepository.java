package com.nus.iss.repository;

import com.nus.iss.entity.AssessmentLearningEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Optional;

public interface AssessmentLearningEventRepository extends JpaRepository<AssessmentLearningEvent, UUID> {
    long countByUserId(Long userId);
    Optional<AssessmentLearningEvent> findFirstByUserIdAndScoreIsNotNullOrderByScoreDesc(Long userId);
    long countByUserIdAndCompletedAtGreaterThanEqual(Long userId, LocalDateTime completedAt);
}
