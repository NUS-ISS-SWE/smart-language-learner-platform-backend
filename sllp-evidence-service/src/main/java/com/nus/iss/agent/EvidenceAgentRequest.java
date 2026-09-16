package com.nus.iss.agent;

import com.nus.iss.enums.Language;

public record EvidenceAgentRequest(
        Long compositionId,
        Long userId,
        String title,
        String content,
        Language language) {
}
