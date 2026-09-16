package com.nus.iss.agent;

import java.util.ArrayList;
import java.util.List;

/**
 * Guards against hallucinated evidence: every excerpt the model returns must be a verbatim
 * substring of the actual composition. Anything that isn't gets listed here so
 * {@link com.nus.iss.agent.EvidenceAgentService} can ask the model to correct it.
 */
public final class ReflectionStep {

    private ReflectionStep() {
    }

    public static List<String> findInvalidExcerpts(EvidenceAgentResponse response, String content) {
        List<String> invalid = new ArrayList<>();
        if (response.criteria() == null || content == null) {
            return invalid;
        }
        for (EvidenceAgentResponse.CriterionFinding criterion : response.criteria()) {
            if (criterion.evidence() == null) {
                continue;
            }
            for (EvidenceAgentResponse.ExcerptFinding item : criterion.evidence()) {
                String excerpt = item.excerpt();
                if (excerpt == null || excerpt.isBlank() || !content.contains(excerpt.trim())) {
                    invalid.add(criterion.criterion() + ": \"" + excerpt + "\"");
                }
            }
        }
        return invalid;
    }
}
