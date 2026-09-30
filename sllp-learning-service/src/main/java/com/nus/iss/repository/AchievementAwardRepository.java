package com.nus.iss.repository;

import com.nus.iss.entity.AchievementAward;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;

public interface AchievementAwardRepository extends JpaRepository<AchievementAward, Long> {
    List<AchievementAward> findByUserIdOrderByAwardedAtDesc(Long userId);
    boolean existsByUserIdAndAchievementDefinitionId(Long userId, Long definitionId);
    List<AchievementAward> findByUserIdAndAchievementDefinitionIdIn(Long userId, Collection<Long> definitionIds);
}
