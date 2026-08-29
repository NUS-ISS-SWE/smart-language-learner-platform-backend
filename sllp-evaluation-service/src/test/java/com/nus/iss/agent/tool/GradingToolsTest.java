package com.nus.iss.agent.tool;

import com.nus.iss.dto.RubricCriterionDTO;
import com.nus.iss.dto.UserProfileDTO;
import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.Language;
import com.nus.iss.service.RubricCriterionClient;
import com.nus.iss.service.UserProfileClient;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GradingToolsTest {

    @Test
    void springAiCallbacksCallServicesInRequiredOrderUsingTrustedContext() {
        UserProfileClient userProfileClient = mock(UserProfileClient.class);
        RubricCriterionClient rubricCriterionClient = mock(RubricCriterionClient.class);
        UserDifficultyTool difficultyTool = new UserDifficultyTool(userProfileClient);
        MarkingRubricTool rubricTool = new MarkingRubricTool(rubricCriterionClient);
        GradingToolContext gradingContext = new GradingToolContext(42L, Language.CHINESE);
        ToolContext toolContext = new ToolContext(Map.of(GradingToolContext.CONTEXT_KEY, gradingContext));
        List<RubricCriterionDTO> rubric = List.of(criterion());
        when(userProfileClient.getUserProfile(42L))
                .thenReturn(new UserProfileDTO(42L, "learner", DifficultyLevel.ADVANCED));
        when(rubricCriterionClient.getRubricCriteria(Language.CHINESE, DifficultyLevel.ADVANCED))
                .thenReturn(rubric);
        Map<String, ToolCallback> callbacks = Arrays.stream(ToolCallbacks.from(difficultyTool, rubricTool))
                .collect(Collectors.toMap(
                        callback -> callback.getToolDefinition().name(),
                        Function.identity()));

        String difficultyResult = callbacks.get("getUserDifficultyLevel").call("{}", toolContext);
        String rubricResult = callbacks.get("getMarkingRubric").call("{}", toolContext);

        assertTrue(difficultyResult.contains("ADVANCED"));
        assertTrue(rubricResult.contains("LANGUAGE_USE"));
        gradingContext.verifyRequiredToolsCalled();
        verify(userProfileClient).getUserProfile(42L);
        verify(rubricCriterionClient).getRubricCriteria(Language.CHINESE, DifficultyLevel.ADVANCED);
    }

    @Test
    void rubricToolCannotRunBeforeDifficultyTool() {
        RubricCriterionClient rubricCriterionClient = mock(RubricCriterionClient.class);
        MarkingRubricTool rubricTool = new MarkingRubricTool(rubricCriterionClient);
        ToolContext toolContext = new ToolContext(Map.of(
                GradingToolContext.CONTEXT_KEY,
                new GradingToolContext(42L, Language.ENGLISH)));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class, () -> rubricTool.getMarkingRubric(toolContext));

        assertEquals("getUserDifficultyLevel must be called before getMarkingRubric", exception.getMessage());
        verifyNoInteractions(rubricCriterionClient);
    }

    @Test
    void gradingContextRejectsAResultWhenRequiredToolsWereSkipped() {
        GradingToolContext gradingContext = new GradingToolContext(42L, Language.ENGLISH);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class, gradingContext::verifyRequiredToolsCalled);

        assertEquals("Grading agent did not complete the required tool calls", exception.getMessage());
    }

    private static RubricCriterionDTO criterion() {
        return new RubricCriterionDTO(
                1L,
                Language.CHINESE,
                DifficultyLevel.ADVANCED,
                "LANGUAGE_USE",
                "Assess precise grammar and vocabulary.",
                new BigDecimal("25.00"),
                null,
                null,
                null,
                null);
    }
}
