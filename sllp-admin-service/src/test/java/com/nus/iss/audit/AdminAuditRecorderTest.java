package com.nus.iss.audit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminAuditRecorderTest {

    @Mock
    private AuditEventService service;

    @Test
    void recordBuildsExpectedAuditRequest() {
        ObjectMapper mapper = new ObjectMapper();
        AdminAuditRecorder recorder = new AdminAuditRecorder(service, mapper);
        UUID requestId = UUID.randomUUID();

        recorder.record("ADMIN_RESOURCE_VIEWED", "ming", "42", "USER", "7",
                requestId.toString(), "GET", "/api/v1/admin/users/7");

        ArgumentCaptor<AuditEventIngestRequest> captor = ArgumentCaptor.forClass(AuditEventIngestRequest.class);
        verify(service).ingest(captor.capture());
        AuditEventIngestRequest request = captor.getValue();
        assertEquals("admin-service", request.service());
        assertEquals("ADMIN", request.category());
        assertEquals("SUCCESS", request.outcome());
        assertEquals(requestId, request.requestId());
        assertSame(mapper.createObjectNode().getClass(), request.metadata().getClass());
    }

    @Test
    void recordTreatsNullAndMalformedRequestIdsAsAbsent() {
        AdminAuditRecorder recorder = new AdminAuditRecorder(service, new ObjectMapper());

        recorder.record("VIEW", null, null, "USER", "1", null, "GET", "/one");
        recorder.record("VIEW", null, null, "USER", "2", "not-a-uuid", "GET", "/two");

        ArgumentCaptor<AuditEventIngestRequest> captor = ArgumentCaptor.forClass(AuditEventIngestRequest.class);
        verify(service, org.mockito.Mockito.times(2)).ingest(captor.capture());
        assertNull(captor.getAllValues().get(0).requestId());
        assertNull(captor.getAllValues().get(1).requestId());
    }

    @Test
    void auditFailureNeverBlocksCaller() {
        AdminAuditRecorder recorder = new AdminAuditRecorder(service, new ObjectMapper());
        doThrow(new RuntimeException("database unavailable")).when(service).ingest(any());

        assertDoesNotThrow(() -> recorder.record("VIEW", "ming", "42", "USER", "7",
                null, "GET", "/api/v1/admin/users/7"));
    }
}
