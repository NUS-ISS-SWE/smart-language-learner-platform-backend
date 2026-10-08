package com.nus.iss.audit;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditEventServiceTest {

    @Mock
    private AuditEventRepository repository;

    private ObjectMapper objectMapper;
    private AuditEventService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new AuditEventService(repository, objectMapper);
    }

    @Test
    void ingestPersistsProvidedValuesAndStructuredJson() throws Exception {
        UUID id = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-09-19T00:00:00Z");
        JsonNode before = objectMapper.readTree("{\"status\":\"OLD\"}");
        JsonNode after = objectMapper.readTree("{\"status\":\"NEW\"}");
        JsonNode metadata = objectMapper.readTree("{\"source\":\"test\"}");
        AuditEventIngestRequest request = request(id, occurredAt, "SUCCESS", before, after, metadata);
        when(repository.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuditEventResponse response = service.ingest(request);

        assertEquals(id, response.id());
        assertEquals(occurredAt, response.occurredAt());
        assertEquals("OLD", response.beforeData().get("status").asString());
        assertEquals("NEW", response.afterData().get("status").asString());
        assertEquals("test", response.metadata().get("source").asString());
    }

    @Test
    void ingestGeneratesIdentityTimestampAndEmptyMetadataWhenAbsent() {
        AuditEventIngestRequest request = request(null, null, "FAILURE", null, null, null);
        when(repository.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuditEventResponse response = service.ingest(request);

        assertNotNull(response.id());
        assertNotNull(response.occurredAt());
        assertEquals(null, response.beforeData());
        assertEquals(null, response.afterData());
        assertTrue(response.metadata().isObject());
    }

    @Test
    void ingestRejectsInvalidOutcomeAndUnsafePayloads() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> service.ingest(request(null, null, "UNKNOWN", null, null, null)));

        JsonNode unsafe = objectMapper.readTree("{\"nested\":{\"authorization\":\"secret\"}}");
        assertThrows(IllegalArgumentException.class,
                () -> service.ingest(request(null, null, "DENIED", unsafe, null, null)));
    }

    @Test
    void ingestWrapsJsonSerializationFailures() throws Exception {
        ObjectMapper failingMapper = org.mockito.Mockito.mock(ObjectMapper.class);
        JsonNode node = org.mockito.Mockito.mock(JsonNode.class);
        when(failingMapper.writeValueAsString(node)).thenThrow(new RuntimeException("cannot serialize"));
        AuditEventService failingService = new AuditEventService(repository, failingMapper);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> failingService.ingest(request(null, null, "SUCCESS", node, null, node)));

        assertEquals("audit JSON could not be serialized", error.getMessage());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void searchAppliesEveryFilterAndClampsPageSize() {
        Instant from = Instant.parse("2026-09-01T00:00:00Z");
        Instant to = Instant.parse("2026-10-01T00:00:00Z");
        UUID requestId = UUID.randomUUID();
        Root<AuditEvent> root = org.mockito.Mockito.mock(Root.class);
        CriteriaQuery<?> query = org.mockito.Mockito.mock(CriteriaQuery.class);
        CriteriaBuilder builder = org.mockito.Mockito.mock(CriteriaBuilder.class);
        Path path = org.mockito.Mockito.mock(Path.class);
        Predicate predicate = org.mockito.Mockito.mock(Predicate.class);
        when(root.get(any(String.class))).thenReturn(path);
        doReturn(predicate).when(builder).greaterThanOrEqualTo(any(Expression.class), any(Comparable.class));
        doReturn(predicate).when(builder).lessThanOrEqualTo(any(Expression.class), any(Comparable.class));
        doReturn(predicate).when(builder).equal(any(Expression.class), any(Object.class));
        when(builder.and(any(Predicate[].class))).thenReturn(predicate);
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenAnswer(invocation -> {
            Specification<AuditEvent> specification = invocation.getArgument(0);
            specification.toPredicate(root, query, builder);
            Pageable pageable = invocation.getArgument(1);
            return new PageImpl<>(List.of(event()), pageable, 1);
        });

        Page<AuditEventResponse> result = service.search(from, to, "42", "USER", "7", "UPDATED",
                "ADMIN", "SUCCESS", requestId, "corr-1", -5, 500);

        assertEquals(0, result.getPageable().getPageNumber());
        assertEquals(100, result.getPageable().getPageSize());
        assertEquals("occurredAt: DESC", result.getPageable().getSort().toString());
        assertEquals("UPDATED", result.getContent().getFirst().action());
        verify(builder).greaterThanOrEqualTo(path, from);
        verify(builder).lessThanOrEqualTo(path, to);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void searchWithNoFiltersUsesMinimumPageSize() {
        CriteriaBuilder builder = org.mockito.Mockito.mock(CriteriaBuilder.class);
        Predicate predicate = org.mockito.Mockito.mock(Predicate.class);
        when(builder.and(any(Predicate[].class))).thenReturn(predicate);
        when(repository.findAll(any(Specification.class), any(Pageable.class))).thenAnswer(invocation -> {
            Specification<AuditEvent> specification = invocation.getArgument(0);
            specification.toPredicate(org.mockito.Mockito.mock(Root.class),
                    org.mockito.Mockito.mock(CriteriaQuery.class), builder);
            Pageable pageable = invocation.getArgument(1);
            return new PageImpl<>(List.of(), pageable, 0);
        });

        Page<AuditEventResponse> result = service.search(null, null, " ", null, "", null,
                null, null, null, null, 2, 0);

        assertEquals(2, result.getPageable().getPageNumber());
        assertEquals(1, result.getPageable().getPageSize());
    }

    private static AuditEventIngestRequest request(UUID id, Instant occurredAt, String outcome,
            JsonNode before, JsonNode after, JsonNode metadata) {
        return new AuditEventIngestRequest(id, occurredAt, "admin-service", "ADMIN", "USER_UPDATED", outcome,
                "USER", "42", "ming", "ADMIN", "USER", "7", UUID.randomUUID(), "corr-1",
                "127.0.0.1", "JUnit", "PATCH", "/api/v1/admin/users/7", 200,
                before, after, metadata, null);
    }

    private static AuditEvent event() {
        return AuditEvent.builder().id(UUID.randomUUID()).occurredAt(Instant.now()).service("admin-service")
                .category("ADMIN").action("UPDATED").outcome("SUCCESS").actorType("USER")
                .metadata("{}").build();
    }
}
