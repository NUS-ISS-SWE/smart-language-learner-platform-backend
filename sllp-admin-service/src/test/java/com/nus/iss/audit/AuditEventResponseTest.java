package com.nus.iss.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

class AuditEventResponseTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void returnsPersistedJsonAsStructuredData() {
        AuditEvent event = AuditEvent.builder()
                .id(UUID.randomUUID()).occurredAt(Instant.parse("2026-09-19T00:00:00Z"))
                .service("test-service").category("SYSTEM").action("TEST_EVENT")
                .outcome("SUCCESS").actorType("SYSTEM")
                .metadata("{\"status\":\"ACTIVE\"}").build();

        AuditEventResponse response = AuditEventResponse.from(event, mapper);

        assertEquals("ACTIVE", response.metadata().get("status").asString());
    }

    @Test
    void handlesNullJsonAndRejectsCorruptStoredJson() {
        AuditEvent nullJson = AuditEvent.builder().id(UUID.randomUUID()).occurredAt(Instant.now())
                .service("test").category("SYSTEM").action("TEST").outcome("SUCCESS")
                .actorType("SYSTEM").metadata(null).build();
        AuditEvent corruptJson = AuditEvent.builder().id(UUID.randomUUID()).occurredAt(Instant.now())
                .service("test").category("SYSTEM").action("TEST").outcome("SUCCESS")
                .actorType("SYSTEM").metadata("not-json").build();

        assertNull(AuditEventResponse.from(nullJson, mapper).metadata());
        assertThrows(IllegalStateException.class, () -> AuditEventResponse.from(corruptJson, mapper));
    }
}
