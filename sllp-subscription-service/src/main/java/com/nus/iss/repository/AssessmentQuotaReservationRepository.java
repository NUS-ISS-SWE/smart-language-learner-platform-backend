package com.nus.iss.repository;

import com.nus.iss.entity.AssessmentQuotaReservation;
import com.nus.iss.enums.AssessmentQuotaStatus;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssessmentQuotaReservationRepository extends JpaRepository<AssessmentQuotaReservation, Long> {
    Optional<AssessmentQuotaReservation> findByRequestId(UUID requestId);
    @Query("""
            SELECT COUNT(r) FROM AssessmentQuotaReservation r
            WHERE r.userId = :userId AND r.cycleStart = :cycleStart
              AND (r.status = com.nus.iss.enums.AssessmentQuotaStatus.CONSUMED
                   OR (r.status = com.nus.iss.enums.AssessmentQuotaStatus.RESERVED AND r.expiresAt > :now))
            """)
    long countActiveUsage(@Param("userId") Long userId, @Param("cycleStart") LocalDate cycleStart,
                          @Param("now") java.time.LocalDateTime now);
}
