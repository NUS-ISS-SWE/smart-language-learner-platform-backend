package com.nus.iss.contracts.grading;

/** A verified excerpt already supplied by the approved grading result. */
public record SuggestionEvidence(String excerpt, String location) {
}
