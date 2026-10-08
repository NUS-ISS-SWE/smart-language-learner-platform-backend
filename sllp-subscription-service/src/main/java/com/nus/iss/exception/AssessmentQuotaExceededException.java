package com.nus.iss.exception;

import java.time.LocalDate;

public class AssessmentQuotaExceededException extends RuntimeException {
    private final LocalDate renewsAt;
    public AssessmentQuotaExceededException(LocalDate renewsAt) {
        super("ASSESSMENT_QUOTA_EXHAUSTED");
        this.renewsAt = renewsAt;
    }
    public LocalDate renewsAt() { return renewsAt; }
}
