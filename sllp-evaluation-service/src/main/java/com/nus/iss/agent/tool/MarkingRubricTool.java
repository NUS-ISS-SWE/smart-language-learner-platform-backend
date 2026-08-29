package com.nus.iss.agent.tool;

import com.nus.iss.dto.RubricCriterionDTO;
import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.service.RubricCriterionClient;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MarkingRubricTool {

    private final RubricCriterionClient rubricCriterionClient;

    public MarkingRubricTool(RubricCriterionClient rubricCriterionClient) {
        this.rubricCriterionClient = rubricCriterionClient;
    }

    @Tool(
            name = "getMarkingRubric",
            description = "Get the authoritative weighted marking rubric from Admin Service for the "
                    + "composition language and the difficulty returned by getUserDifficultyLevel."
    )
    public List<RubricCriterionDTO> getMarkingRubric(ToolContext toolContext) {
        GradingToolContext gradingContext = GradingToolContext.from(toolContext);
        DifficultyLevel difficultyLevel = gradingContext.requireDifficultyLevel();
        List<RubricCriterionDTO> criteria = rubricCriterionClient.getRubricCriteria(
                gradingContext.language(), difficultyLevel);
        gradingContext.recordRubricRetrieved();
        return criteria;
    }
}
