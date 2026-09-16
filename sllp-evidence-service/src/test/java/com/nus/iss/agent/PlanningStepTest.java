package com.nus.iss.agent;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlanningStepTest {

    @Test
    void trimsAndKeepsNonBlankSteps() {
        assertEquals(
                List.of("Read the composition once", "Match excerpts to each criterion"),
                PlanningStep.normalize(List.of("  Read the composition once  ", "", "Match excerpts to each criterion", "   ")));
    }

    @Test
    void fallsBackWhenModelReturnedNoPlan() {
        assertEquals(List.of("No explicit plan was returned by the model."), PlanningStep.normalize(null));
        assertEquals(List.of("No explicit plan was returned by the model."), PlanningStep.normalize(List.of("   ", "")));
    }
}
