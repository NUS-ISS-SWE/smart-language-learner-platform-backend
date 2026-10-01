package com.nus.iss.repository;

import com.nus.iss.model.GradingAnchor;
import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.Language;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GradingAnchorRepository extends JpaRepository<GradingAnchor, Long> {
    @Query("""
            SELECT anchor
            FROM GradingAnchor anchor
            WHERE anchor.language = :language
              AND anchor.difficultyLevel = :difficultyLevel
            ORDER BY function('random')
            """)
    List<GradingAnchor> findRandomByLanguageAndDifficultyLevel(
            @Param("language") Language language,
            @Param("difficultyLevel") DifficultyLevel difficultyLevel,
            Pageable pageable);
}
