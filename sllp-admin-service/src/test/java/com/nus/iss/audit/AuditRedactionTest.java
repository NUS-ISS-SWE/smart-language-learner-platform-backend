package com.nus.iss.audit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

class AuditRedactionTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void permitsSafeMetadata() throws Exception {
        assertDoesNotThrow(() -> AuditRedaction.assertSafe(mapper.readTree("{\"status\":\"ACTIVE\",\"field\":\"role\"}")));
    }

    @Test
    void rejectsSecretsAndLearnerContent() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> AuditRedaction.assertSafe(mapper.readTree("{\"password\":\"never-log\"}")));
        assertThrows(IllegalArgumentException.class, () -> AuditRedaction.assertSafe(mapper.readTree("{\"content\":\"learner work\"}")));
    }

    @Test
    void handlesNullsArraysAndNormalizedForbiddenNames() throws Exception {
        assertDoesNotThrow(() -> AuditRedaction.assertSafe(null));
        assertDoesNotThrow(() -> AuditRedaction.assertSafe(mapper.readTree("null")));
        assertDoesNotThrow(() -> AuditRedaction.assertSafe(mapper.readTree("[{\"status\":\"OK\"}]")));
        assertThrows(IllegalArgumentException.class,
                () -> AuditRedaction.assertSafe(mapper.readTree("[{\"payment-reference\":\"hidden\"}]")));
        assertThrows(IllegalArgumentException.class,
                () -> AuditRedaction.assertSafe(mapper.readTree("{\"s3_key\":\"hidden\"}")));
    }
}
