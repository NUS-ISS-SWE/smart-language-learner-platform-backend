package com.nus.iss.agent.tool;

import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.Language;
import org.springframework.ai.chat.model.ToolContext;

public final class GradingToolContext {

    public static final String CONTEXT_KEY = GradingToolContext.class.getName();

    private final Long userId;
    private final Language language;
    private DifficultyLevel difficultyLevel;
    private boolean rubricRetrieved;

    public GradingToolContext(Long userId, Language language) {
        this.userId = userId;
        this.language = language;
    }

    public Long userId() {
        return userId;
    }

    public Language language() {
        return language;
    }

    public void recordDifficultyLevel(DifficultyLevel difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public DifficultyLevel requireDifficultyLevel() {
        if (difficultyLevel == null) {
            throw new IllegalStateException(
                    "getUserDifficultyLevel must be called before getMarkingRubric");
        }
        return difficultyLevel;
    }

    public void recordRubricRetrieved() {
        requireDifficultyLevel();
        this.rubricRetrieved = true;
    }

    public void verifyRequiredToolsCalled() {
        if (difficultyLevel == null || !rubricRetrieved) {
            throw new IllegalStateException("Grading agent did not complete the required tool calls");
        }
    }

    public static GradingToolContext from(ToolContext toolContext) {
        Object context = toolContext.getContext().get(CONTEXT_KEY);
        if (context instanceof GradingToolContext gradingToolContext) {
            return gradingToolContext;
        }
        throw new IllegalStateException("Grading tool context is missing");
    }
}
