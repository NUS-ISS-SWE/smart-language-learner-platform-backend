package com.nus.iss.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditEventControllerTest {

    @Mock
    private AuditEventService service;
    @Mock
    private AuditSignatureVerifier signatures;
    @Mock
    private ObjectMapper objectMapper;

    private AuditEventController controller;

    @BeforeEach
    void setUp() {
        controller = new AuditEventController(service, signatures, objectMapper);
    }

    @Test
    void ingestVerifiesSignatureDeserializesAndReturnsCreated() throws Exception {
        String body = "{\"service\":\"admin-service\"}";
        AuditEventIngestRequest request = request();
        AuditEventResponse saved = response();
        when(objectMapper.readValue(body, AuditEventIngestRequest.class)).thenReturn(request);
        when(service.ingest(request)).thenReturn(saved);

        ResponseEntity<AuditEventResponse> result = controller.ingest("123", "signature", body);

        assertEquals(201, result.getStatusCode().value());
        assertSame(saved, result.getBody());
        verify(signatures).verify("123", "signature", body);
    }

    @Test
    void searchAsAdminDelegatesEveryArgument() {
        Instant from = Instant.parse("2026-09-01T00:00:00Z");
        Instant to = Instant.parse("2026-10-01T00:00:00Z");
        UUID requestId = UUID.randomUUID();
        Page<AuditEventResponse> page = new PageImpl<>(List.of(response()));
        when(service.search(from, to, "1", "USER", "2", "UPDATE", "ADMIN", "SUCCESS",
                requestId, "corr", 3, 10)).thenReturn(page);

        Page<AuditEventResponse> result = controller.search("ADMIN", from, to, "1", "USER", "2",
                "UPDATE", "ADMIN", "SUCCESS", requestId, "corr", 3, 10);

        assertSame(page, result);
    }

    @Test
    void searchRejectsNonAdminBeforeServiceCall() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> controller.search("USER", null, null, null, null, null, null,
                        null, null, null, null, 0, 25));

        assertEquals(403, error.getStatusCode().value());
        verifyNoInteractions(service);
    }

    private static AuditEventIngestRequest request() {
        return new AuditEventIngestRequest(null, null, "admin-service", "ADMIN", "VIEW", "SUCCESS",
                "USER", null, null, null, null, null, null, null, null, null,
                "GET", "/api/v1/admin", 200, null, null, null, null);
    }

    private static AuditEventResponse response() {
        return new AuditEventResponse(UUID.randomUUID(), Instant.now(), "admin-service", "ADMIN", "VIEW",
                "SUCCESS", "USER", null, null, null, null, null, null, null, null, null,
                "GET", "/api/v1/admin", 200, null, null, null, null);
    }
}
