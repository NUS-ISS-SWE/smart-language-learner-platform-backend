package com.nus.iss.exception;

public class AssessmentQuotaExceededException extends RuntimeException {
    public AssessmentQuotaExceededException() { super("ASSESSMENT_QUOTA_EXHAUSTED"); }
}
