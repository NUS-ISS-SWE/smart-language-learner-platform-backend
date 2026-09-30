package com.nus.iss.dto;

import com.nus.iss.enums.ChallengeCadence;

public record ChallengeResponse(
        Long id, ChallengeCadence cadence, String language, String category, String themePath, String title, String prompt,
        int minResponseUnits, int maxResponseUnits, String cycleKey, boolean submitted) { }
