package com.nus.iss.service;

import com.nus.iss.dto.AnchorPaperResponse;
import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.Language;

import java.util.List;

public interface AnchorService {
    List<AnchorPaperResponse> getAnchorPapers(Language language, DifficultyLevel difficultyLevel);
}
