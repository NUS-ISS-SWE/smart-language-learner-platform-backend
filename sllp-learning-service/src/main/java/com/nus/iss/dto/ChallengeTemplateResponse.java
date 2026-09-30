package com.nus.iss.dto;

import com.nus.iss.enums.ChallengeCadence;

/** Public admin representation; keeps persistence details out of the HTTP contract. */
public record ChallengeTemplateResponse(
        Long id,
        String code,
        ChallengeCadence cadence,
        String language,
        String category,
        String themePath,
        String title,
        String prompt,
        int minResponseUnits,
        int maxResponseUnits,
        boolean active,
        int displayOrder) { }
