package com.nus.iss.audit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditEventControllerTest {

    private static final String BODY = """
            {"service":"user-service","category":"ADMIN","action":"USER_DELETED","outcome":"SUCCESS","actorType":"USER","actorId":"7"}
            """;

    @Mock
    private AuditEventService service;

    @Mock
    private AuditSignatureVerifier signatures;

    private AuditEventController controller() {
        return new AuditEventController(service, signatures, new ObjectMapper());
    }

    @Test
    void signedEventIsStored() throws Exception {
        AuditEventResponse stored = org.mockito.Mockito.mock(AuditEventResponse.class);
        when(service.ingest(any())).thenReturn(stored);

        ResponseEntity<AuditEventResponse> response = controller().ingest("1790000000", "signature", BODY);

        verify(signatures).verify("1790000000", "signature", BODY);
        ArgumentCaptor<AuditEventIngestRequest> request = ArgumentCaptor.forClass(AuditEventIngestRequest.class);
        verify(service).ingest(request.capture());
        assertEquals("USER_DELETED", request.getValue().action());
        assertEquals("7", request.getValue().actorId());
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(stored, response.getBody());
    }

    @Test
    void badlySignedEventIsNotStored() {
        doThrow(new SecurityException("invalid audit signature")).when(signatures).verify("1790000000", "forged", BODY);

        assertThrows(SecurityException.class, () -> controller().ingest("1790000000", "forged", BODY));
        verify(service, never()).ingest(any());
    }

    @Test
    void searchIsForAdminsOnly() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> controller().search(
                "USER_PREMIUM", null, null, null, null, null, null, null, null, null, null, 0, 25));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verifyNoInteractions(service);
    }

    @Test
    void adminSearchPassesEveryFilterThrough() {
        Instant from = Instant.parse("2026-09-01T00:00:00Z");
        Instant to = Instant.parse("2026-09-30T00:00:00Z");
        UUID requestId = UUID.randomUUID();
        Page<AuditEventResponse> page = Page.empty();
        when(service.search(from, to, "7", "USER", "12", "USER_DELETED", "ADMIN", "SUCCESS", requestId, "corr-1", 1, 50))
                .thenReturn(page);

        Page<AuditEventResponse> result = controller().search("ADMIN", from, to, "7", "USER", "12", "USER_DELETED",
                "ADMIN", "SUCCESS", requestId, "corr-1", 1, 50);

        assertSame(page, result);
    }
}
