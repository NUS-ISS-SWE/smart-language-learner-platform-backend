package com.nus.iss.audit;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditEventServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Mock
    private AuditEventRepository repository;

    private AuditEventService service() {
        return new AuditEventService(repository, mapper);
    }

    @Test
    void ingestStoresTheEventAsSent() {
        when(repository.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UUID id = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-09-01T10:00:00Z");
        ObjectNode after = mapper.createObjectNode().put("status", "INACTIVE");
        ObjectNode metadata = mapper.createObjectNode().put("reason", "spam");

        AuditEventResponse response = service().ingest(request(id, occurredAt, "SUCCESS", requestId, after, metadata));

        ArgumentCaptor<AuditEvent> saved = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repository).save(saved.capture());
        AuditEvent event = saved.getValue();
        assertEquals(id, event.getId());
        assertEquals(occurredAt, event.getOccurredAt());
        assertEquals("user-service", event.getService());
        assertEquals("USER_STATUS_CHANGED", event.getAction());
        assertEquals("SUCCESS", event.getOutcome());
        assertEquals("7", event.getActorId());
        assertEquals(requestId, event.getRequestId());
        assertEquals(200, event.getHttpStatus());
        assertNull(event.getBeforeData());
        assertEquals("{\"status\":\"INACTIVE\"}", event.getAfterData());
        assertEquals("{\"reason\":\"spam\"}", event.getMetadata());

        assertEquals(id, response.id());
        assertEquals("INACTIVE", response.afterData().get("status").asString());
    }

    @Test
    void ingestFillsInIdTimeAndMetadataWhenMissing() {
        when(repository.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Instant before = Instant.now();

        service().ingest(request(null, null, "DENIED", null, null, null));

        ArgumentCaptor<AuditEvent> saved = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repository).save(saved.capture());
        assertNotNull(saved.getValue().getId());
        assertNotNull(saved.getValue().getOccurredAt());
        assertEquals(false, saved.getValue().getOccurredAt().isBefore(before));
        assertEquals("{}", saved.getValue().getMetadata());
    }

    @Test
    void ingestRejectsAnUnknownOutcome() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service().ingest(request(null, null, "MAYBE", null, null, null)));

        assertEquals("invalid audit outcome", error.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void ingestRejectsSecretsInThePayload() {
        ObjectNode leaky = mapper.createObjectNode().put("password", "hunter2");

        assertThrows(IllegalArgumentException.class,
                () -> service().ingest(request(null, null, "SUCCESS", null, leaky, null)));
        verify(repository, never()).save(any());
    }

    @Test
    void searchClampsPagingAndSortsNewestFirst() {
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        service().search(null, null, null, null, null, null, null, null, null, null, -3, 500);
        service().search(null, null, null, null, null, null, null, null, null, null, 2, 0);

        ArgumentCaptor<Pageable> pages = ArgumentCaptor.forClass(Pageable.class);
        verify(repository, org.mockito.Mockito.times(2)).findAll(any(Specification.class), pages.capture());
        Pageable first = pages.getAllValues().get(0);
        assertEquals(0, first.getPageNumber());
        assertEquals(100, first.getPageSize());
        assertEquals(Sort.by(Sort.Direction.DESC, "occurredAt"), first.getSort());
        Pageable second = pages.getAllValues().get(1);
        assertEquals(2, second.getPageNumber());
        assertEquals(1, second.getPageSize());
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchFiltersOnlyByTheGivenCriteria() {
        AuditEvent stored = AuditEvent.builder().id(UUID.randomUUID()).occurredAt(Instant.now())
                .service("user-service").category("ADMIN").action("USER_DELETED").outcome("SUCCESS")
                .actorType("USER").metadata("{}").build();
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(stored)));
        Instant from = Instant.parse("2026-09-01T00:00:00Z");
        Instant to = Instant.parse("2026-09-30T00:00:00Z");
        UUID requestId = UUID.randomUUID();

        Page<AuditEventResponse> result = service().search(from, to, "7", " ", null, "USER_DELETED", "ADMIN", "SUCCESS",
                requestId, "corr-1", 0, 25);

        assertEquals("USER_DELETED", result.getContent().getFirst().action());

        // Run the captured specification against mocked criteria objects to see which filters it adds.
        ArgumentCaptor<Specification<AuditEvent>> spec = ArgumentCaptor.forClass(Specification.class);
        verify(repository).findAll(spec.capture(), any(Pageable.class));
        Root<AuditEvent> root = mock(Root.class);
        CriteriaBuilder builder = mock(CriteriaBuilder.class);
        Path<Object> path = mock(Path.class);
        when(root.get(anyString())).thenReturn(path);

        spec.getValue().toPredicate(root, mock(CriteriaQuery.class), builder);

        verify(builder).greaterThanOrEqualTo(any(Path.class), org.mockito.ArgumentMatchers.eq(from));
        verify(builder).lessThanOrEqualTo(any(Path.class), org.mockito.ArgumentMatchers.eq(to));
        verify(builder).equal(path, "7");
        verify(builder).equal(path, "USER_DELETED");
        verify(builder).equal(path, "ADMIN");
        verify(builder).equal(path, "SUCCESS");
        verify(builder).equal(path, requestId);
        verify(builder).equal(path, "corr-1");
        // Blank and null criteria add no filter.
        verify(root, never()).get("targetType");
        verify(root, never()).get("targetId");
        verify(builder).and(any(Predicate[].class));
    }

    private static AuditEventIngestRequest request(UUID id, Instant occurredAt, String outcome, UUID requestId,
                                                   ObjectNode afterData, ObjectNode metadata) {
        return new AuditEventIngestRequest(id, occurredAt, "user-service", "ADMIN", "USER_STATUS_CHANGED", outcome,
                "USER", "7", "alextan", "ADMIN", "USER", "12", requestId, null, "10.0.0.1", "JUnit",
                "PATCH", "/api/v1/admin/users/12/status", 200, null, afterData, metadata, null);
    }
}
