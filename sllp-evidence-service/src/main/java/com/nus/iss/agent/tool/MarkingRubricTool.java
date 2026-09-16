package com.nus.iss.agent.tool;

import com.nus.iss.client.RubricCriterionClient;
import com.nus.iss.dto.RubricCriterionDTO;
import com.nus.iss.enums.DifficultyLevel;
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
                    + "composition language and the difficulty returned by getUserDifficultyLevel. "
                    + "You must find evidence for every criterion this returns."
    )
    public List<RubricCriterionDTO> getMarkingRubric(ToolContext toolContext) {
        EvidenceToolContext evidenceContext = EvidenceToolContext.from(toolContext);
        DifficultyLevel difficultyLevel = evidenceContext.requireDifficultyLevel();
        List<RubricCriterionDTO> criteria = rubricCriterionClient.getRubricCriteria(
                evidenceContext.language(), difficultyLevel);
        evidenceContext.recordRubric(criteria);
        return criteria;
    }
}
