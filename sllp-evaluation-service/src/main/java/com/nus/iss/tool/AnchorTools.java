package com.nus.iss.tool;

import com.nus.iss.dto.AnchorPaperResponse;
import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.Language;
import com.nus.iss.service.AnchorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class AnchorTools {
    public static final String REQUEST_ID_CONTEXT_KEY = "requestId";
    public static final String COMPOSITION_ID_CONTEXT_KEY = "compositionId";
    public static final String ATTEMPT_CONTEXT_KEY = "attempt";

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
    public List<AnchorPaperResponse> getAnchorPapers(Language language,
                                                     DifficultyLevel difficultyLevel,
                                                     ToolContext toolContext) {
        long startedAt = System.nanoTime();
        Object requestId = toolContext.getContext().get(REQUEST_ID_CONTEXT_KEY);
        Object compositionId = toolContext.getContext().get(COMPOSITION_ID_CONTEXT_KEY);
        Object attempt = toolContext.getContext().get(ATTEMPT_CONTEXT_KEY);
        log.info("AI tool call started tool=getAnchorPapers requestId={} compositionId={} attempt={} language={} difficultyLevel={}",
                requestId, compositionId, attempt, language, difficultyLevel);
        try {
            List<AnchorPaperResponse> result = anchorService.getAnchorPapers(language, difficultyLevel);
            log.info("AI tool call completed tool=getAnchorPapers requestId={} compositionId={} attempt={} resultCount={} durationMs={}",
                    requestId, compositionId, attempt, result.size(), elapsedMillis(startedAt));
            return result;
        } catch (RuntimeException error) {
            log.warn("AI tool call failed tool=getAnchorPapers requestId={} compositionId={} attempt={} category={} durationMs={}",
                    requestId, compositionId, attempt, error.getClass().getSimpleName(), elapsedMillis(startedAt));
            throw error;
        }
    }

    private static long elapsedMillis(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }
}
