package com.nus.iss.repository;

import com.nus.iss.entity.LearningGoal;
import com.nus.iss.enums.GoalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LearningGoalRepository extends JpaRepository<LearningGoal, Long> {
    List<LearningGoal> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<LearningGoal> findByUserIdAndStatus(Long userId, GoalStatus status);
    Optional<LearningGoal> findFirstByUserIdAndFocusedTrue(Long userId);
}
