package com.nus.iss.dto;

/** Attempt reliability and provider-reported usage for an assessment agent. */
public record AgentMetrics(
        String agent,
        long attempts,
        long failedAttempts,
        Double failurePercent,
        long aiTokensUsed,
        long estimatedAiCostMicros) {
}
