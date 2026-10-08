package com.nus.iss.repository;

import com.nus.iss.entity.AchievementDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AchievementDefinitionRepository extends JpaRepository<AchievementDefinition, Long> {
    List<AchievementDefinition> findByActiveTrue();
    boolean existsByCode(String code);
}
