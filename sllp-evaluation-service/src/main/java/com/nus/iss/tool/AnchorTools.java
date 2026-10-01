package com.nus.iss.tool;

import com.nus.iss.dto.AnchorPaperResponse;
import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.Language;
import com.nus.iss.service.AnchorService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AnchorTools {
    private final AnchorService anchorService;

    public AnchorTools(AnchorService anchorService) {
        this.anchorService = anchorService;
    }

    @Tool(
            name = "getAnchorPapers",
            description = """
                    Retrieve manually graded Anchor Papers for the requested language
                    and difficulty level.

                    Use these examples as calibration references when grading the
                    current composition.

                    Anchor Paper scores are raw 1-5 score-band references only.
                    Use them to calibrate bandScore, but do not copy an Anchor
                    Paper score automatically. Java calculates percentage scores.
                    """
    )
    public List<AnchorPaperResponse> getAnchorPapers(Language language, DifficultyLevel difficultyLevel) {
        return anchorService.getAnchorPapers(language, difficultyLevel);
    }
}
