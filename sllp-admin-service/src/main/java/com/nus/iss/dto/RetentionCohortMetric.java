package com.nus.iss.dto;

import java.time.LocalDate;

/** Weekly registration cohort with meaningful-activity retention counts. */
public record RetentionCohortMetric(
        LocalDate cohortStart,
        long cohortSize,
        Long week0Retained,
        Long week1Retained,
        Long week2Retained,
        Long week3Retained,
        Long week4Retained) {
}
