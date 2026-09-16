package com.nus.iss.agent.tool;

import com.nus.iss.dto.RubricCriterionDTO;
import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.Language;
import org.springframework.ai.chat.model.ToolContext;

import java.util.List;

/**
 * Same enforced-order pattern as evaluation-service's GradingToolContext: the model cannot
 * fetch the rubric before the difficulty level, and the run isn't valid unless both happened.
 * Also carries the rubric it fetched (needed for the reflection retry prompt) and the
 * compositionId (needed by PriorEvidenceTool).
 */
public final class EvidenceToolContext {

    public static final String CONTEXT_KEY = EvidenceToolContext.class.getName();

    private final Long userId;
    private final Long compositionId;
    private final Language language;
    private DifficultyLevel difficultyLevel;
    private List<RubricCriterionDTO> rubric;

    public EvidenceToolContext(Long userId, Long compositionId, Language language) {
        this.userId = userId;
        this.compositionId = compositionId;
        this.language = language;
    }

    public Long userId() {
        return userId;
    }

    public Long compositionId() {
        return compositionId;
    }

    public Language language() {
        return language;
    }

    public void recordDifficultyLevel(DifficultyLevel difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public DifficultyLevel requireDifficultyLevel() {
        if (difficultyLevel == null) {
            throw new IllegalStateException("getUserDifficultyLevel must be called before getMarkingRubric");
        }
        return difficultyLevel;
    }

    public void recordRubric(List<RubricCriterionDTO> rubric) {
        requireDifficultyLevel();
        this.rubric = rubric;
    }

    public List<RubricCriterionDTO> requireRubric() {
        if (rubric == null) {
            throw new IllegalStateException("getMarkingRubric was not called");
        }
        return rubric;
    }

    public void verifyRequiredToolsCalled() {
        if (difficultyLevel == null || rubric == null) {
            throw new IllegalStateException("Evidence agent did not complete the required tool calls");
        }
    }

    public static EvidenceToolContext from(ToolContext toolContext) {
        Object context = toolContext.getContext().get(CONTEXT_KEY);
        if (context instanceof EvidenceToolContext evidenceToolContext) {
            return evidenceToolContext;
        }
        throw new IllegalStateException("Evidence tool context is missing");
    }
}
