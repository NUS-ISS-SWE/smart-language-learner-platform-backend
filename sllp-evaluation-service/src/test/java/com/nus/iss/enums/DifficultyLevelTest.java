package com.nus.iss.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DifficultyLevelTest {
    @Test
    void mapsDifficultyLevelsToSingaporePrimaryStandards() {
        assertEquals("Singapore Primary 1 to 2", DifficultyLevel.BEGINNER.getGradingStandard());
        assertEquals("Singapore Primary 3 to 4", DifficultyLevel.INTERMEDIATE.getGradingStandard());
        assertEquals("Singapore Primary 5 to 6", DifficultyLevel.ADVANCED.getGradingStandard());
    }
}
