package com.nus.iss.dto;

import java.time.LocalDate;

/** Weekly registration cohort with meaningful-activity retention counts. */
public record RetentionCohortMetric(
        LocalDate cohortStart,
        long cohortSize,
        long week0Retained,
        long week1Retained,
        long week2Retained,
        long week3Retained,
        long week4Retained) {
}
