package com.nus.iss.tool;

import com.nus.iss.dto.AnchorPaperResponse;
import com.nus.iss.dto.AnchorScoreResponse;
import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.Language;
import com.nus.iss.service.AnchorService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnchorToolsTest {

    @Test
    void delegatesAnchorLookupToService() {
        AnchorService service = mock(AnchorService.class);
        List<AnchorPaperResponse> expected = List.of();
        when(service.getAnchorPapers(Language.ENGLISH, DifficultyLevel.INTERMEDIATE)).thenReturn(expected);
        GradingToolTrace trace = new GradingToolTrace();

        List<AnchorPaperResponse> result = new AnchorTools(service)
                .getAnchorPapers(Language.ENGLISH, DifficultyLevel.INTERMEDIATE,
                        new ToolContext(Map.of(
                                AnchorTools.REQUEST_ID_CONTEXT_KEY, "request-1",
                                AnchorTools.COMPOSITION_ID_CONTEXT_KEY, 10L,
                                AnchorTools.ATTEMPT_CONTEXT_KEY, 1,
                                AnchorTools.TRACE_CONTEXT_KEY, trace)));

        assertSame(expected, result);
        assertTrue(trace.wasAnchorToolCalled());
        assertEquals(expected, trace.getAnchorPapers());
        verify(service).getAnchorPapers(Language.ENGLISH, DifficultyLevel.INTERMEDIATE);
    }

    @Test
    void returnsAnchorsWhenTraceIsAbsent() {
        AnchorService service = mock(AnchorService.class);
        List<AnchorPaperResponse> anchors = List.of(new AnchorPaperResponse(
                1L, "Strong sample", "A clear composition", Language.ENGLISH,
                DifficultyLevel.BEGINNER, List.of(new AnchorScoreResponse("TASK_IDEAS", 4))));
        when(service.getAnchorPapers(Language.ENGLISH, DifficultyLevel.BEGINNER)).thenReturn(anchors);

        List<AnchorPaperResponse> result = new AnchorTools(service).getAnchorPapers(
                Language.ENGLISH, DifficultyLevel.BEGINNER, new ToolContext(Map.of()));

        assertSame(anchors, result);
    }

    @Test
    void propagatesAnchorLookupFailure() {
        AnchorService service = mock(AnchorService.class);
        when(service.getAnchorPapers(Language.CHINESE, DifficultyLevel.ADVANCED))
                .thenThrow(new IllegalStateException("repository unavailable"));

        assertThrows(IllegalStateException.class, () -> new AnchorTools(service).getAnchorPapers(
                Language.CHINESE, DifficultyLevel.ADVANCED, new ToolContext(Map.of())));
    }
}
