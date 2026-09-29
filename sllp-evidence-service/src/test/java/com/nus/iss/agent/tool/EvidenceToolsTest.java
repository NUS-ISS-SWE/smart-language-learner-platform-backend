package com.nus.iss.agent.tool;

import com.nus.iss.client.RubricCriterionClient;
import com.nus.iss.client.UserProfileClient;
import com.nus.iss.dto.EvidenceCollectionResult;
import com.nus.iss.dto.RubricCriterionDTO;
import com.nus.iss.dto.UserProfileDTO;
import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.Language;
import com.nus.iss.memory.EvidenceMemoryStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The tools the evidence agent calls, and the order they are enforced in: difficulty level first,
 * then the rubric for that level; a run that skipped either is invalid.
 */
@ExtendWith(MockitoExtension.class)
class EvidenceToolsTest {

    private static final List<RubricCriterionDTO> RUBRIC = List.of(new RubricCriterionDTO(1L, Language.ENGLISH,
            DifficultyLevel.INTERMEDIATE, "Organization", "Logical order", new BigDecimal("40.00"), null, null, null, null));

    @Mock
    private UserProfileClient userProfileClient;

    @Mock
    private RubricCriterionClient rubricCriterionClient;

    private final EvidenceToolContext context = new EvidenceToolContext(7L, 12L, Language.ENGLISH);
    private final ToolContext toolContext = new ToolContext(Map.of(EvidenceToolContext.CONTEXT_KEY, context));

    @Test
    void difficultyThenRubricCompletesTheRequiredCalls() {
        when(userProfileClient.getUserProfile(7L)).thenReturn(new UserProfileDTO(7L, "ming", DifficultyLevel.INTERMEDIATE));
        when(rubricCriterionClient.getRubricCriteria(Language.ENGLISH, DifficultyLevel.INTERMEDIATE)).thenReturn(RUBRIC);

        assertEquals(DifficultyLevel.INTERMEDIATE, new UserDifficultyTool(userProfileClient).getUserDifficultyLevel(toolContext));
        assertSame(RUBRIC, new MarkingRubricTool(rubricCriterionClient).getMarkingRubric(toolContext));

        assertSame(RUBRIC, context.requireRubric());
        assertDoesNotThrow(context::verifyRequiredToolsCalled);
    }

    @Test
    void rubricCannotBeFetchedBeforeTheDifficultyLevel() {
        assertThrows(IllegalStateException.class,
                () -> new MarkingRubricTool(rubricCriterionClient).getMarkingRubric(toolContext));
        verifyNoInteractions(rubricCriterionClient);
    }

    @Test
    void runThatSkippedTheToolsIsInvalid() {
        assertThrows(IllegalStateException.class, context::verifyRequiredToolsCalled);
        assertThrows(IllegalStateException.class, context::requireRubric);

        context.recordDifficultyLevel(DifficultyLevel.BEGINNER);

        assertThrows(IllegalStateException.class, context::verifyRequiredToolsCalled, "rubric still missing");
    }

    @Test
    void contextCarriesTheRequestIdentity() {
        assertEquals(7L, context.userId());
        assertEquals(12L, context.compositionId());
        assertEquals(Language.ENGLISH, context.language());
    }

    @Test
    void toolsFailWithoutAnEvidenceContext() {
        ToolContext empty = new ToolContext(Map.of());

        assertThrows(IllegalStateException.class, () -> new UserDifficultyTool(userProfileClient).getUserDifficultyLevel(empty));
        verifyNoInteractions(userProfileClient);
    }

    @Test
    void priorEvidenceSummarisesTheLastRun() {
        EvidenceMemoryStore memory = new EvidenceMemoryStore();
        memory.put(12L, new EvidenceCollectionResult(12L, List.of("Scan paragraphs", "Quote evidence"), List.of(), null));

        String summary = new PriorEvidenceTool(memory).getPriorEvidence(toolContext);

        assertEquals("Previous run found evidence for 0 criteria. Its plan was: Scan paragraphs; Quote evidence", summary);
    }

    @Test
    void priorEvidenceSaysSoWhenThereIsNone() {
        String summary = new PriorEvidenceTool(new EvidenceMemoryStore()).getPriorEvidence(toolContext);

        assertEquals("No prior evidence was found for this composition.", summary);
    }
}
