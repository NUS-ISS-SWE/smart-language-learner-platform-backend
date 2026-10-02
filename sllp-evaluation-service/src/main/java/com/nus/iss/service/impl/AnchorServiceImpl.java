package com.nus.iss.service.impl;

import com.nus.iss.dto.AnchorPaperResponse;
import com.nus.iss.dto.AnchorScoreResponse;
import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.Language;
import com.nus.iss.model.GradingAnchor;
import com.nus.iss.model.GradingAnchorScore;
import com.nus.iss.repository.GradingAnchorRepository;
import com.nus.iss.service.AnchorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

@Service
public class AnchorServiceImpl implements AnchorService {
    private static final int MAX_ANCHORS = 5;

    private final GradingAnchorRepository anchorRepository;

    public AnchorServiceImpl(GradingAnchorRepository anchorRepository) {
        this.anchorRepository = anchorRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnchorPaperResponse> getAnchorPapers(Language language, DifficultyLevel difficultyLevel) {
        List<GradingAnchor> ordered = anchorRepository
                .findByLanguageAndDifficultyLevelOrderByIdAsc(language, difficultyLevel)
                .stream()
                .filter(anchor -> !anchor.getScores().isEmpty())
                .sorted(Comparator.comparingDouble(AnchorServiceImpl::averageScore)
                        .thenComparing(GradingAnchor::getTitle, Comparator.nullsLast(String::compareTo))
                        .thenComparing(GradingAnchor::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
        return evenlySpaced(ordered).stream()
                .map(AnchorServiceImpl::toResponse)
                .toList();
    }

    private static List<GradingAnchor> evenlySpaced(List<GradingAnchor> ordered) {
        if (ordered.size() <= MAX_ANCHORS) {
            return ordered;
        }
        return IntStream.range(0, MAX_ANCHORS)
                .map(index -> index * (ordered.size() - 1) / (MAX_ANCHORS - 1))
                .mapToObj(ordered::get)
                .toList();
    }

    private static double averageScore(GradingAnchor anchor) {
        return anchor.getScores().stream()
                .mapToInt(GradingAnchorScore::getScore)
                .average()
                .orElseThrow();
    }

    private static AnchorPaperResponse toResponse(GradingAnchor anchor) {
        List<AnchorScoreResponse> scores = anchor.getScores().stream()
                .sorted(Comparator.comparing(GradingAnchorScore::getCriterionName))
                .map(score -> new AnchorScoreResponse(score.getCriterionName(), score.getScore()))
                .toList();
        return new AnchorPaperResponse(anchor.getId(), anchor.getTitle(), anchor.getComposition(),
                anchor.getLanguage(), anchor.getDifficultyLevel(), scores);
    }
}
