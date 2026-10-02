package com.nus.iss.repository;

import com.nus.iss.model.GradingAnchor;
import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.Language;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GradingAnchorRepository extends JpaRepository<GradingAnchor, Long> {
    @EntityGraph(attributePaths = "scores")
    List<GradingAnchor> findByLanguageAndDifficultyLevelOrderByIdAsc(
            Language language, DifficultyLevel difficultyLevel);
}
