package com.nus.iss.dto;

/**
 * One piece of evidence for a criterion. {@code verifiedOnFirstPass} is false whenever the
 * reflection step had to ask the model for a correction — a ready-made traceability signal for
 * "how much did we have to double-check this run".
 */
public record EvidenceItem(
        String excerpt,
        String location,
        boolean verifiedOnFirstPass) {
}
