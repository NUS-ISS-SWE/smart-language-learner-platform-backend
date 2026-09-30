package com.nus.iss.service;

import java.util.UUID;

public interface AssessmentQuotaClient {
    void reserve(Long userId, UUID requestId);
    void settle(UUID requestId, boolean consumed);

    static AssessmentQuotaClient noop() {
        return new AssessmentQuotaClient() {
            @Override public void reserve(Long userId, UUID requestId) { }
            @Override public void settle(UUID requestId, boolean consumed) { }
        };
    }
}
