package com.nus.iss.agent.tool;

import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.service.UserProfileClient;
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
        GradingToolContext gradingContext = GradingToolContext.from(toolContext);
        DifficultyLevel difficultyLevel = userProfileClient
                .getUserProfile(gradingContext.userId())
                .difficultyLevel();
        gradingContext.recordDifficultyLevel(difficultyLevel);
        return difficultyLevel;
    }
}
