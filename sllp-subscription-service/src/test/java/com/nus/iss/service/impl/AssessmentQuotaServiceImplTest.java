package com.nus.iss.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nus.iss.entity.AppUserRef;
import com.nus.iss.entity.AssessmentQuotaReservation;
import com.nus.iss.entity.PricingPlan;
import com.nus.iss.enums.AssessmentQuotaStatus;
import com.nus.iss.exception.AssessmentQuotaExceededException;
import com.nus.iss.repository.AppUserRoleRepository;
import com.nus.iss.repository.AssessmentQuotaReservationRepository;
import com.nus.iss.repository.UserSubscriptionRepository;
import com.nus.iss.service.PricingPlanService;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssessmentQuotaServiceImplTest {
    @Mock private AssessmentQuotaReservationRepository reservations;
    @Mock private UserSubscriptionRepository subscriptions;
    @Mock private PricingPlanService pricingPlans;
    @Mock private AppUserRoleRepository appUsers;

    private AssessmentQuotaServiceImpl service() {
        return new AssessmentQuotaServiceImpl(reservations, subscriptions, pricingPlans, appUsers);
    }

    private static PricingPlan freePlan(int limit) {
        return PricingPlan.builder().code("FREE_TRIAL").displayName("Free").priceMinor(0L)
                .currency("SGD").monthlyAssessmentLimit(limit).build();
    }

    @Test
    void reserves_before_work_is_enqueued_and_records_the_plan_limit() {
        when(appUsers.findByIdForUpdate(7L)).thenReturn(Optional.of(new AppUserRef()));
        when(reservations.findByRequestId(any())).thenReturn(Optional.empty());
        when(subscriptions.findFirstByUserIdAndStatusOrderByCreatedAtDesc(any(), any())).thenReturn(Optional.empty());
        when(pricingPlans.findFreePlan()).thenReturn(freePlan(20));
        when(reservations.countActiveUsage(eq(7L), any(LocalDate.class), any())).thenReturn(0L);
        when(reservations.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var reserved = service().reserve(7L, UUID.randomUUID());

        assertThat(reserved.planCode()).isEqualTo("FREE_TRIAL");
        assertThat(reserved.monthlyAssessmentLimit()).isEqualTo(20);
        assertThat(reserved.assessmentsUsed()).isEqualTo(1);
        ArgumentCaptor<AssessmentQuotaReservation> saved = ArgumentCaptor.forClass(AssessmentQuotaReservation.class);
        verify(reservations).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(AssessmentQuotaStatus.RESERVED);
        verify(appUsers).findByIdForUpdate(7L);
    }

    @Test
    void rejects_when_reserved_and_consumed_work_reaches_the_plan_limit() {
        when(appUsers.findByIdForUpdate(7L)).thenReturn(Optional.of(new AppUserRef()));
        when(reservations.findByRequestId(any())).thenReturn(Optional.empty());
        when(subscriptions.findFirstByUserIdAndStatusOrderByCreatedAtDesc(any(), any())).thenReturn(Optional.empty());
        when(pricingPlans.findFreePlan()).thenReturn(freePlan(1));
        when(reservations.countActiveUsage(eq(7L), any(LocalDate.class), any())).thenReturn(1L);

        assertThatThrownBy(() -> service().reserve(7L, UUID.randomUUID()))
                .isInstanceOf(AssessmentQuotaExceededException.class);

        verify(reservations, never()).save(any());
    }
}
