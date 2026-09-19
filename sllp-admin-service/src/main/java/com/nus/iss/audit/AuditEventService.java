package com.nus.iss.audit;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import jakarta.persistence.criteria.Predicate;

@Service
public class AuditEventService {
    private final AuditEventRepository repository;
    private final ObjectMapper objectMapper;

    public AuditEventService(AuditEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AuditEventResponse ingest(AuditEventIngestRequest request) {
        validate(request);
        AuditEvent event = AuditEvent.builder()
                .id(request.id() == null ? UUID.randomUUID() : request.id())
                .occurredAt(request.occurredAt() == null ? Instant.now() : request.occurredAt())
                .service(request.service()).category(request.category()).action(request.action())
                .outcome(request.outcome()).actorType(request.actorType()).actorId(request.actorId())
                .actorName(request.actorName()).actorRole(request.actorRole()).targetType(request.targetType())
                .targetId(request.targetId()).requestId(request.requestId()).correlationId(request.correlationId())
                .sourceIp(request.sourceIp()).userAgent(request.userAgent()).httpMethod(request.httpMethod())
                .httpPath(request.httpPath()).httpStatus(request.httpStatus()).beforeData(request.beforeData())
                .afterData(request.afterData()).metadata(request.metadata() == null ? objectMapper.createObjectNode() : request.metadata())
                .errorCode(request.errorCode()).build();
        return AuditEventResponse.from(repository.save(event));
    }

    @Transactional(readOnly = true)
    public Page<AuditEventResponse> search(Instant from, Instant to, String actorId, String targetType, String targetId,
            String action, String category, String outcome, UUID requestId, String correlationId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100), Sort.by(Sort.Direction.DESC, "occurredAt"));
        Specification<AuditEvent> specification = (root, query, builder) -> {
            java.util.List<Predicate> predicates = new java.util.ArrayList<>();
            if (from != null) predicates.add(builder.greaterThanOrEqualTo(root.get("occurredAt"), from));
            if (to != null) predicates.add(builder.lessThanOrEqualTo(root.get("occurredAt"), to));
            equal(predicates, builder, root, "actorId", actorId); equal(predicates, builder, root, "targetType", targetType);
            equal(predicates, builder, root, "targetId", targetId); equal(predicates, builder, root, "action", action);
            equal(predicates, builder, root, "category", category); equal(predicates, builder, root, "outcome", outcome);
            if (requestId != null) predicates.add(builder.equal(root.get("requestId"), requestId));
            equal(predicates, builder, root, "correlationId", correlationId);
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        return repository.findAll(specification, pageable).map(AuditEventResponse::from);
    }

    private static void equal(java.util.List<Predicate> predicates, jakarta.persistence.criteria.CriteriaBuilder builder,
            jakarta.persistence.criteria.Root<AuditEvent> root, String field, String value) {
        if (value != null && !value.isBlank()) predicates.add(builder.equal(root.get(field), value));
    }

    private static void validate(AuditEventIngestRequest request) {
        if (!SetValues.OUTCOMES.contains(request.outcome())) throw new IllegalArgumentException("invalid audit outcome");
        AuditRedaction.assertSafe(request.beforeData()); AuditRedaction.assertSafe(request.afterData()); AuditRedaction.assertSafe(request.metadata());
    }

    private static final class SetValues { private static final java.util.Set<String> OUTCOMES = java.util.Set.of("SUCCESS", "FAILURE", "DENIED"); }
}
