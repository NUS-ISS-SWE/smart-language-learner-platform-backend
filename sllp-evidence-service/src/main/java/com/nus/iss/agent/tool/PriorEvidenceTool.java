package com.nus.iss.agent.tool;

import com.nus.iss.dto.EvidenceCollectionResult;
import com.nus.iss.memory.EvidenceMemoryStore;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class PriorEvidenceTool {

    private final EvidenceMemoryStore memoryStore;

    public PriorEvidenceTool(EvidenceMemoryStore memoryStore) {
        this.memoryStore = memoryStore;
    }

    @Tool(
            name = "getPriorEvidence",
            description = "Get a summary of evidence collected on a previous run for this same "
                    + "composition, if any. Optional — call it only if staying consistent with an "
                    + "earlier pass would help."
    )
    public String getPriorEvidence(ToolContext toolContext) {
        EvidenceToolContext evidenceContext = EvidenceToolContext.from(toolContext);
        return memoryStore.get(evidenceContext.compositionId())
                .map(PriorEvidenceTool::summarize)
                .orElse("No prior evidence was found for this composition.");
    }

    private static String summarize(EvidenceCollectionResult result) {
        return "Previous run found evidence for %d criteria. Its plan was: %s"
                .formatted(result.criteria().size(), String.join("; ", result.plan()));
    }
}
