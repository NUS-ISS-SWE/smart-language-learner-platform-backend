package com.nus.iss.service.impl;

import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.Language;
import com.nus.iss.model.GradingAnchor;
import com.nus.iss.model.GradingAnchorScore;
import com.nus.iss.repository.GradingAnchorRepository;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnchorServiceImplTest {

    @Test
    void mapsAnchorAndScoresToToolResponse() {
        GradingAnchorRepository repository = mock(GradingAnchorRepository.class);
        GradingAnchor anchor = GradingAnchor.builder()
                .id(1L)
                .language(Language.ENGLISH)
                .difficultyLevel(DifficultyLevel.INTERMEDIATE)
                .title("A Visit to the Zoo")
                .composition("Last Saturday...")
                .build();
        anchor.addScore(GradingAnchorScore.builder()
                .criterionName("TASK_IDEAS")
                .score(3)
                .build());
        when(repository.findByLanguageAndDifficultyLevelOrderByIdAsc(
                Language.ENGLISH, DifficultyLevel.INTERMEDIATE))
                .thenReturn(List.of(anchor));

        var response = new AnchorServiceImpl(repository)
                .getAnchorPapers(Language.ENGLISH, DifficultyLevel.INTERMEDIATE);

        assertEquals(1, response.size());
        assertEquals(1L, response.getFirst().anchorId());
        assertEquals("A Visit to the Zoo", response.getFirst().title());
        assertEquals("TASK_IDEAS", response.getFirst().scores().getFirst().criterionName());
        assertEquals(3, response.getFirst().scores().getFirst().score());
    }

    @Test
    void returnsEmptyListWhenNoAnchorsMatch() {
        GradingAnchorRepository repository = mock(GradingAnchorRepository.class);
        when(repository.findByLanguageAndDifficultyLevelOrderByIdAsc(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of());

        var response = new AnchorServiceImpl(repository)
                .getAnchorPapers(Language.CHINESE, DifficultyLevel.ADVANCED);

        assertEquals(List.of(), response);
    }

    @Test
    void returnsStableLowToHighCalibrationSetWhenMoreThanFiveAnchorsExist() {
        GradingAnchorRepository repository = mock(GradingAnchorRepository.class);
        List<GradingAnchor> anchors = java.util.stream.IntStream.rangeClosed(1, 9)
                .mapToObj(index -> anchor(index, (index + 1) / 2))
                .toList();
        when(repository.findByLanguageAndDifficultyLevelOrderByIdAsc(
                Language.ENGLISH, DifficultyLevel.ADVANCED)).thenReturn(anchors.reversed());

        var response = new AnchorServiceImpl(repository)
                .getAnchorPapers(Language.ENGLISH, DifficultyLevel.ADVANCED);

        assertEquals(5, response.size());
        assertEquals(List.of(1, 2, 3, 4, 5), response.stream()
                .map(item -> item.scores().getFirst().score())
                .toList());
        assertEquals(List.of(1L, 3L, 5L, 7L, 9L), response.stream()
                .map(item -> item.anchorId())
                .toList());
    }

    private static GradingAnchor anchor(long id, int score) {
        GradingAnchor anchor = GradingAnchor.builder()
                .id(id)
                .language(Language.ENGLISH)
                .difficultyLevel(DifficultyLevel.ADVANCED)
                .title("Anchor " + id)
                .composition("Composition " + id)
                .build();
        anchor.addScore(GradingAnchorScore.builder()
                .criterionName("TASK_IDEAS")
                .score(score)
                .build());
        return anchor;
    }
}
