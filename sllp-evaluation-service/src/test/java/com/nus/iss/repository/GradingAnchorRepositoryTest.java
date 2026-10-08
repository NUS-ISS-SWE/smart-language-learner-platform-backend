package com.nus.iss.repository;

import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.Language;
import com.nus.iss.model.GradingAnchor;
import com.nus.iss.model.GradingAnchorScore;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class GradingAnchorRepositoryTest {

    @Autowired
    private GradingAnchorRepository anchorRepository;

    @Autowired
    private GradingAnchorScoreRepository scoreRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsAnchorWithCriterionScores() {
        GradingAnchor anchor = anchor();
        anchor.addScore(score("GRAMMAR_SENTENCE_CONTROL", 3));
        anchor.addScore(score("VOCABULARY_WORD_CHOICE", 4));

        GradingAnchor saved = anchorRepository.saveAndFlush(anchor);
        entityManager.clear();

        GradingAnchor reloaded = anchorRepository.findById(saved.getId()).orElseThrow();
        assertNotNull(reloaded.getCreatedAt());
        assertNotNull(reloaded.getUpdatedAt());
        assertEquals(2, reloaded.getScores().size());
        assertEquals(2, scoreRepository.count());
        reloaded.getScores().forEach(item -> {
            assertNotNull(item.getId());
            assertNotNull(item.getCreatedAt());
            assertNotNull(item.getUpdatedAt());
        });
    }

    @Test
    void allowsDuplicateCriterionNamesForSameAnchor() {
        GradingAnchor anchor = anchorRepository.saveAndFlush(anchor());
        scoreRepository.saveAndFlush(score(anchor, "GRAMMAR_SENTENCE_CONTROL", 3));
        scoreRepository.saveAndFlush(score(anchor, "GRAMMAR_SENTENCE_CONTROL", 4));

        assertEquals(2, scoreRepository.count());
    }

    @Test
    void findsAllMatchingAnchorsInStableIdOrderWithScores() {
        for (int index = 0; index < 7; index++) {
            GradingAnchor matching = anchor(Language.ENGLISH, DifficultyLevel.INTERMEDIATE, "Matching " + index);
            matching.addScore(score("TASK_IDEAS", (index % 5) + 1));
            anchorRepository.save(matching);
        }
        anchorRepository.save(anchor(Language.CHINESE, DifficultyLevel.INTERMEDIATE, "Wrong language"));
        anchorRepository.save(anchor(Language.ENGLISH, DifficultyLevel.ADVANCED, "Wrong difficulty"));
        anchorRepository.flush();
        entityManager.clear();

        var results = anchorRepository.findByLanguageAndDifficultyLevelOrderByIdAsc(
                Language.ENGLISH, DifficultyLevel.INTERMEDIATE);

        assertEquals(7, results.size());
        assertTrue(results.stream().allMatch(item -> item.getLanguage() == Language.ENGLISH));
        assertTrue(results.stream().allMatch(
                item -> item.getDifficultyLevel() == DifficultyLevel.INTERMEDIATE));
        assertTrue(results.stream().allMatch(item -> !item.getScores().isEmpty()));
        assertEquals(results.stream().map(GradingAnchor::getId).sorted().toList(),
                results.stream().map(GradingAnchor::getId).toList());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 6})
    void rejectsScoresOutsideAllowedRange(int value) {
        GradingAnchor anchor = anchorRepository.saveAndFlush(anchor());

        assertThrows(DataIntegrityViolationException.class,
                () -> scoreRepository.saveAndFlush(score(anchor, "GRAMMAR_SENTENCE_CONTROL", value)));
    }

    private static GradingAnchor anchor() {
        return anchor(Language.ENGLISH, DifficultyLevel.INTERMEDIATE, "A Visit to the Zoo");
    }

    private static GradingAnchor anchor(Language language, DifficultyLevel difficultyLevel, String title) {
        return GradingAnchor.builder()
                .language(language)
                .difficultyLevel(difficultyLevel)
                .title(title)
                .composition("We visited the zoo and saw many animals.")
                .build();
    }

    private static GradingAnchorScore score(String criterionName, int value) {
        return GradingAnchorScore.builder()
                .criterionName(criterionName)
                .score(value)
                .build();
    }

    private static GradingAnchorScore score(GradingAnchor anchor, String criterionName, int value) {
        return GradingAnchorScore.builder()
                .anchor(anchor)
                .criterionName(criterionName)
                .score(value)
                .build();
    }
}
