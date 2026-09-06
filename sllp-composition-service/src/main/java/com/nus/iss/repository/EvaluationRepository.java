package com.nus.iss.repository;

import com.nus.iss.entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    List<Evaluation> findByCompositionIdOrderByEvaluatedAtDesc(Long compositionId);

    /**
     * Atomically records a completion once per grading request. Kafka can redeliver
     * messages concurrently, so a read-before-write existence check is insufficient.
     */
    @Modifying
    @Query(value = """
            INSERT INTO evaluation (composition_id, request_id, requested_at, score, feedback, suggestion_status, suggestions, success, failure_reason, evaluated_at)
            VALUES (:compositionId, :requestId, :requestedAt, :score, :feedback, :suggestionStatus, :suggestions, :success, :failureReason, :evaluatedAt)
            ON CONFLICT (request_id) WHERE request_id IS NOT NULL DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("compositionId") long compositionId,
            @Param("requestId") UUID requestId,
            @Param("requestedAt") LocalDateTime requestedAt,
            @Param("score") BigDecimal score,
            @Param("feedback") String feedback,
            @Param("suggestionStatus") String suggestionStatus,
            @Param("suggestions") String suggestions,
            @Param("success") boolean success,
            @Param("failureReason") String failureReason,
            @Param("evaluatedAt") LocalDateTime evaluatedAt);
}
