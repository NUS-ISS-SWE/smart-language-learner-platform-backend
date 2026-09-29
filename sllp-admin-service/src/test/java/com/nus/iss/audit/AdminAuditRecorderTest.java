package com.nus.iss.audit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuditRecorderTest {

    @Mock
    private AuditEventService auditEvents;

    private AdminAuditRecorder recorder() {
        return new AdminAuditRecorder(auditEvents, new ObjectMapper());
    }

    @Test
    void recordsASuccessfulAdminAction() {
        UUID requestId = UUID.randomUUID();

        recorder().record("RUBRIC_DELETED", "alextan", "17", "RUBRIC_CRITERION", "4",
                requestId.toString(), "DELETE", "/api/v1/admin/rubrics/4");

        ArgumentCaptor<AuditEventIngestRequest> captured = ArgumentCaptor.forClass(AuditEventIngestRequest.class);
        verify(auditEvents).ingest(captured.capture());
        AuditEventIngestRequest request = captured.getValue();
        assertEquals("admin-service", request.service());
        assertEquals("ADMIN", request.category());
        assertEquals("RUBRIC_DELETED", request.action());
        assertEquals("SUCCESS", request.outcome());
        assertEquals("17", request.actorId());
        assertEquals("alextan", request.actorName());
        assertEquals("ADMIN", request.actorRole());
        assertEquals("RUBRIC_CRITERION", request.targetType());
        assertEquals("4", request.targetId());
        assertEquals(requestId, request.requestId());
        assertEquals("DELETE", request.httpMethod());
        assertTrue(request.metadata().isEmpty());
    }

    @Test
    void requestIdThatIsNotAUuidIsDropped() {
        recorder().record("ADMIN_RESOURCE_VIEWED", "alextan", "17", "HTTP_RESOURCE", "/x", "not-a-uuid", "GET", "/x");

        ArgumentCaptor<AuditEventIngestRequest> captured = ArgumentCaptor.forClass(AuditEventIngestRequest.class);
        verify(auditEvents).ingest(captured.capture());
        assertNull(captured.getValue().requestId());
    }

    @Test
    void auditStoreFailureNeverBlocksTheAdminAction() {
        when(auditEvents.ingest(any())).thenThrow(new IllegalStateException("database down"));

        assertDoesNotThrow(() -> recorder().record("RUBRIC_DELETED", "alextan", "17", "RUBRIC_CRITERION", "4",
                null, "DELETE", "/api/v1/admin/rubrics/4"));
    }
}
