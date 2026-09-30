package com.nus.iss.repository;

import com.nus.iss.entity.ChallengeSubmission;
import com.nus.iss.enums.ChallengeCadence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChallengeSubmissionRepository extends JpaRepository<ChallengeSubmission, Long> {
    Optional<ChallengeSubmission> findByUserIdAndCadenceAndCycleKey(Long userId, ChallengeCadence cadence, String cycleKey);
    boolean existsByChallengeTemplateId(Long challengeTemplateId);
    long countByUserId(Long userId);
    long countByUserIdAndCadence(Long userId, ChallengeCadence cadence);
    long countByUserIdAndCategorySnapshotAndSubmittedAtGreaterThanEqual(Long userId, String categorySnapshot, LocalDateTime startedAt);
    long countByUserIdAndCadenceAndSubmittedAtGreaterThanEqual(Long userId, ChallengeCadence cadence, LocalDateTime startedAt);
    @Query("select distinct s.activityDate from ChallengeSubmission s where s.userId = :userId order by s.activityDate desc")
    List<LocalDate> findActivityDates(Long userId);
}
