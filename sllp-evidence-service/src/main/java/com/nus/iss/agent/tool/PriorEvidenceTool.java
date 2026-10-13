package com.nus.iss.agent.tool;

import com.nus.iss.dto.CriterionEvidence;
import com.nus.iss.dto.EvidenceCollectionResult;
import com.nus.iss.dto.EvidenceItem;
import com.nus.iss.memory.EvidenceMemoryStore;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * Cross-run memory for reassessment: when the orchestrator sends a composition back to Evidence
 * (low grading confidence or a REGRADE review), the model can see exactly what it reported last
 * time and either stay consistent or correct it.
 */
@Component
public class PriorEvidenceTool {

    private final EvidenceMemoryStore memoryStore;

    public PriorEvidenceTool(EvidenceMemoryStore memoryStore) {
        this.memoryStore = memoryStore;
    }

    @Tool(
            name = "getPriorEvidence",
            description = "Get the evidence collected on a previous run for this same composition, "
                    + "if any: the plan and every finding per criterion. Optional — call it when this "
                    + "may be a reassessment. Use it to stay consistent or to correct earlier findings; "
                    + "re-check every excerpt against the composition and do not copy it blindly."
    )
    public String getPriorEvidence(ToolContext toolContext) {
        EvidenceToolContext evidenceContext = EvidenceToolContext.from(toolContext);
        return memoryStore.get(evidenceContext.compositionId())
                .map(PriorEvidenceTool::summarize)
                .orElse("No prior evidence was found for this composition.");
    }

    static String summarize(EvidenceCollectionResult result) {
        StringBuilder summary = new StringBuilder("Previous run plan: ")
                .append(String.join("; ", result.plan()))
                .append("\nPrevious findings:");
        for (CriterionEvidence criterion : result.criteria()) {
            summary.append("\n- ").append(criterion.criterion());
            for (EvidenceItem item : criterion.evidence()) {
                summary.append("\n  [").append(item.polarity()).append("] \"")
                        .append(item.excerpt()).append("\" — ").append(item.note());
            }
        }
        return summary.toString();
    }
}
