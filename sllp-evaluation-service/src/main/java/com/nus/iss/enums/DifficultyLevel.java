package com.nus.iss.enums;

import lombok.Getter;

@Getter
public enum DifficultyLevel {
    BEGINNER("Singapore Primary 1 to 2"),
    INTERMEDIATE("Singapore Primary 3 to 4"),
    ADVANCED("Singapore Primary 5 to 6");

    private final String gradingStandard;

    DifficultyLevel(String gradingStandard) {
        this.gradingStandard = gradingStandard;
    }
}
