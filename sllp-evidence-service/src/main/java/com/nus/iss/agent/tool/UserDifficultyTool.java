package com.nus.iss.agent.tool;

import com.nus.iss.client.UserProfileClient;
import com.nus.iss.enums.DifficultyLevel;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class UserDifficultyTool {

    private final UserProfileClient userProfileClient;

    public UserDifficultyTool(UserProfileClient userProfileClient) {
        this.userProfileClient = userProfileClient;
    }

    @Tool(
            name = "getUserDifficultyLevel",
            description = "Get the student's difficulty level from User Service. "
                    + "This must be called before getMarkingRubric."
    )
    public DifficultyLevel getUserDifficultyLevel(ToolContext toolContext) {
        EvidenceToolContext evidenceContext = EvidenceToolContext.from(toolContext);
        DifficultyLevel difficultyLevel = userProfileClient
                .getUserProfile(evidenceContext.userId())
                .difficultyLevel();
        evidenceContext.recordDifficultyLevel(difficultyLevel);
        return difficultyLevel;
    }
}
