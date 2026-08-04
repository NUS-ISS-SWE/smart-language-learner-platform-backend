package com.nus.iss.repository;

import com.nus.iss.entity.RubricCriterion;
import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.Language;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RubricCriterionRepository extends JpaRepository<RubricCriterion, Long> {

    List<RubricCriterion> findByLanguage(Language language, Sort sort);

    List<RubricCriterion> findByDifficultyLevel(DifficultyLevel difficultyLevel, Sort sort);

    List<RubricCriterion> findByLanguageAndDifficultyLevel(
            Language language, DifficultyLevel difficultyLevel, Sort sort);
}
