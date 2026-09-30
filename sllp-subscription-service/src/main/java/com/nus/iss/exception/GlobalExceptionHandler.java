package com.nus.iss.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AssessmentQuotaExceededException.class)
    public ProblemDetail handleQuotaExceeded(AssessmentQuotaExceededException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        problem.setTitle("Assessment quota exhausted");
        problem.setProperty("code", "ASSESSMENT_QUOTA_EXHAUSTED");
        problem.setProperty("renewsAt", ex.renewsAt());
        return problem;
    }

    @ExceptionHandler(SubscriptionNotFoundException.class)
    public ProblemDetail handleNotFound(SubscriptionNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Subscription Not Found");
        return problem;
    }

    @ExceptionHandler(PricingPlanNotFoundException.class)
    public ProblemDetail handlePlanNotFound(PricingPlanNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Pricing Plan Not Found");
        return problem;
    }

    @ExceptionHandler(InvalidSubscriptionOperationException.class)
    public ProblemDetail handleInvalidOperation(InvalidSubscriptionOperationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Invalid Subscription Operation");
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error("Unhandled exception in subscription service", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        problem.setTitle("Internal Server Error");
        return problem;
    }
}
