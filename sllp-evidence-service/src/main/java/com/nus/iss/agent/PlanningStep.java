package com.nus.iss.agent;

import java.util.List;

/**
 * The model is asked to emit its short plan before its findings in the same structured
 * response (no second round-trip — cheaper, and still a distinct, inspectable artifact).
 * This just normalizes what comes back.
 */
public final class PlanningStep {

    private PlanningStep() {
    }

    public static List<String> normalize(List<String> plan) {
        if (plan == null) {
            return List.of("No explicit plan was returned by the model.");
        }
        List<String> cleaned = plan.stream()
                .filter(step -> step != null && !step.isBlank())
                .map(String::trim)
                .toList();
        return cleaned.isEmpty() ? List.of("No explicit plan was returned by the model.") : cleaned;
    }
}
