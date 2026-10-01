package com.nus.iss.service.impl;

import com.nus.iss.dto.AnchorPaperResponse;
import com.nus.iss.dto.AnchorScoreResponse;
import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.Language;
import com.nus.iss.model.GradingAnchor;
import com.nus.iss.repository.GradingAnchorRepository;
import com.nus.iss.service.AnchorService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        return anchorRepository.findRandomByLanguageAndDifficultyLevel(
                        language, difficultyLevel, PageRequest.of(0, MAX_ANCHORS))
                .stream()
                .map(AnchorServiceImpl::toResponse)
                .toList();
    }

    private static AnchorPaperResponse toResponse(GradingAnchor anchor) {
        List<AnchorScoreResponse> scores = anchor.getScores().stream()
                .map(score -> new AnchorScoreResponse(score.getCriterionName(), score.getScore()))
                .toList();
        return new AnchorPaperResponse(anchor.getId(), anchor.getTitle(), anchor.getComposition(),
                anchor.getLanguage(), anchor.getDifficultyLevel(), scores);
    }
}
