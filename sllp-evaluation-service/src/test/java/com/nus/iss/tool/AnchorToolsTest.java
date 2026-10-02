package com.nus.iss.tool;

import com.nus.iss.dto.AnchorPaperResponse;
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
}
