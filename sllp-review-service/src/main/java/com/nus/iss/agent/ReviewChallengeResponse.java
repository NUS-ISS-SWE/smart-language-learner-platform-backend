package com.nus.iss.agent;

/** Result from the Review Agent's independent adversarial critic pass. */
public record ReviewChallengeResponse(String verdict, String checkName, String rationale) {}
