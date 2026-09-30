package com.nus.iss.service.impl;

import com.nus.iss.dto.AssessmentQuotaReservationResponse;
import com.nus.iss.entity.AssessmentQuotaReservation;
import com.nus.iss.entity.PricingPlan;
import com.nus.iss.entity.UserSubscription;
import com.nus.iss.enums.AssessmentQuotaStatus;
import com.nus.iss.enums.SubscriptionStatus;
import com.nus.iss.exception.AssessmentQuotaExceededException;
import com.nus.iss.repository.AssessmentQuotaReservationRepository;
import com.nus.iss.repository.AppUserRoleRepository;
import com.nus.iss.repository.UserSubscriptionRepository;
import com.nus.iss.service.AssessmentQuotaService;
import com.nus.iss.service.PricingPlanService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class AssessmentQuotaServiceImpl implements AssessmentQuotaService {
    private final AssessmentQuotaReservationRepository reservations;
    private final UserSubscriptionRepository subscriptions;
    private final PricingPlanService pricingPlans;
    private final AppUserRoleRepository appUsers;

    @Override @Transactional
    public AssessmentQuotaReservationResponse reserve(Long userId, UUID requestId) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate cycleStart = today.withDayOfMonth(1);
        LocalDate renewsAt = cycleStart.plusMonths(1);
        // Lock a stable per-user row before counting. This makes count-plus-insert atomic
        // for a learner while allowing other learners to reserve concurrently.
        appUsers.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("User does not exist"));
        AssessmentQuotaReservation existing = reservations.findByRequestId(requestId).orElse(null);
        if (existing != null) return response(existing, used(userId, cycleStart), renewsAt);
        PricingPlan plan = subscriptions.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, SubscriptionStatus.ACTIVE)
                .map(UserSubscription::getPricingPlan).orElseGet(pricingPlans::findFreePlan);
        int used = used(userId, cycleStart);
        if (used >= plan.getMonthlyAssessmentLimit()) throw new AssessmentQuotaExceededException(renewsAt);
        AssessmentQuotaReservation reservation = reservations.save(AssessmentQuotaReservation.builder()
                .requestId(requestId).userId(userId).planCode(plan.getCode()).cycleStart(cycleStart)
                .assessmentLimit(plan.getMonthlyAssessmentLimit()).status(AssessmentQuotaStatus.RESERVED)
                .expiresAt(LocalDateTime.now(ZoneOffset.UTC).plusHours(24)).createdAt(LocalDateTime.now(ZoneOffset.UTC)).build());
        return response(reservation, used + 1, renewsAt);
    }

    @Override @Transactional
    public void settle(UUID requestId, boolean consumed) {
        reservations.findByRequestId(requestId).ifPresent(reservation -> {
            if (reservation.getStatus() == AssessmentQuotaStatus.RESERVED) {
                reservation.setStatus(consumed ? AssessmentQuotaStatus.CONSUMED : AssessmentQuotaStatus.RELEASED);
            }
        });
    }

    private int used(Long userId, LocalDate cycleStart) {
        // Expired reservations are not chargeable; a failed worker must not consume
        // a learner's monthly allowance indefinitely.
        return Math.toIntExact(reservations.countActiveUsage(userId, cycleStart, LocalDateTime.now(ZoneOffset.UTC)));
    }
    private AssessmentQuotaReservationResponse response(AssessmentQuotaReservation reservation, int used, LocalDate renewsAt) {
        return new AssessmentQuotaReservationResponse(reservation.getRequestId(), reservation.getPlanCode(), reservation.getAssessmentLimit(), used, renewsAt);
    }
}
