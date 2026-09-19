package com.nus.iss.audit;

import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import tools.jackson.databind.JsonNode;

final class AuditRedaction {
    private static final Set<String> FORBIDDEN_TOKENS = Set.of(
            "password", "token", "secret", "authorization", "credential", "content",
            "extractedtext", "extracted_text", "paymentreference", "payment_reference", "s3key", "s3_key");

    private AuditRedaction() { }

    static void assertSafe(JsonNode node) {
        if (node == null || node.isNull()) return;
        if (node.isObject()) {
            Iterator<String> names = node.propertyNames().iterator();
            while (names.hasNext()) {
                String name = names.next();
                String normalized = name.replace("-", "").replace("_", "").toLowerCase(Locale.ROOT);
                if (FORBIDDEN_TOKENS.contains(normalized)) {
                    throw new IllegalArgumentException("audit payload contains prohibited field: " + name);
                }
                assertSafe(node.get(name));
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) assertSafe(child);
        }
    }
}
