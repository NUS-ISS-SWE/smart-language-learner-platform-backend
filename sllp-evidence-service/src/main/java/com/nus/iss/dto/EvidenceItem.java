package com.nus.iss.dto;

import com.nus.iss.enums.EvidencePolarity;

/**
 * One piece of evidence for a criterion. {@code polarity}+{@code note} carry the actual
 * judgment (what's good or bad, and why) — {@code excerpt} alone is just neutral source text.
 * {@code verifiedOnFirstPass} is false whenever the reflection step had to ask the model for a
 * correction — a ready-made traceability signal for "how much did we have to double-check this
 * run".
 */
public record EvidenceItem(
        String excerpt,
        String location,
        EvidencePolarity polarity,
        String note,
        boolean verifiedOnFirstPass) {
}
