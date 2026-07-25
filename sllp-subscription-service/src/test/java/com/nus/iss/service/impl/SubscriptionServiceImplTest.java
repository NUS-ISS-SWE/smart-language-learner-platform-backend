package com.nus.iss.service.impl;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nus.iss.catalog.SubscriptionPlanCatalog;
import com.nus.iss.dto.SelectPlanRequestDTO;
import com.nus.iss.dto.UserSubscriptionDTO;
import com.nus.iss.entity.UserSubscription;
import com.nus.iss.enums.SubscriptionPlan;
import com.nus.iss.enums.SubscriptionStatus;
import com.nus.iss.exception.SubscriptionNotFoundException;
import com.nus.iss.payment.PaymentResult;
import com.nus.iss.payment.PaymentService;
import com.nus.iss.repository.AppUserRoleRepository;
import com.nus.iss.repository.UserSubscriptionRepository;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceImplTest {

    @Mock
    private UserSubscriptionRepository subscriptionRepository;
    @Mock
    private AppUserRoleRepository appUserRoleRepository;
    @Mock
    private PaymentService paymentService;
    @Mock
    private SubscriptionPlanCatalog planCatalog;

    private SubscriptionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SubscriptionServiceImpl(subscriptionRepository, appUserRoleRepository, paymentService, planCatalog);
    }

    @Test
    void selectPlan_freeTrial_setsNullEndDateAndActivatesImmediately() {
        Long userId = 1L;
        when(subscriptionRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(paymentService.processPayment(eq(userId), eq(SubscriptionPlan.FREE_TRIAL), isNull()))
                .thenReturn(PaymentResult.success("SIM-00000001"));
        UserSubscription saved = UserSubscription.builder()
                .id(1L).userId(userId).planType(SubscriptionPlan.FREE_TRIAL)
                .status(SubscriptionStatus.ACTIVE).startDate(LocalDate.now()).build();
        when(subscriptionRepository.save(any())).thenReturn(saved);

        UserSubscriptionDTO result = service.selectPlan(userId, new SelectPlanRequestDTO(SubscriptionPlan.FREE_TRIAL, null));

        assertThat(result.planType()).isEqualTo(SubscriptionPlan.FREE_TRIAL);
        assertThat(result.status()).isEqualTo(SubscriptionStatus.ACTIVE);

        ArgumentCaptor<UserSubscription> captor = ArgumentCaptor.forClass(UserSubscription.class);
        verify(subscriptionRepository).save(captor.capture());
        assertThat(captor.getValue().getEndDate()).isNull();
        assertThat(captor.getValue().getDurationDays()).isNull();
        verify(appUserRoleRepository).updateRole(userId, "USER_FREE_TRIAL");
    }

    @Test
    void selectPlan_basic_setsEndDateThirtyDaysAheadAndUpdatesRole() {
        Long userId = 2L;
        when(subscriptionRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(paymentService.processPayment(eq(userId), eq(SubscriptionPlan.BASIC), isNull()))
                .thenReturn(PaymentResult.success("SIM-00000002"));
        LocalDate today = LocalDate.now();
        UserSubscription saved = UserSubscription.builder()
                .id(2L).userId(userId).planType(SubscriptionPlan.BASIC).status(SubscriptionStatus.ACTIVE)
                .startDate(today).endDate(today.plusDays(30)).durationDays(30).build();
        when(subscriptionRepository.save(any())).thenReturn(saved);

        UserSubscriptionDTO result = service.selectPlan(userId, new SelectPlanRequestDTO(SubscriptionPlan.BASIC, null));

        assertThat(result.planType()).isEqualTo(SubscriptionPlan.BASIC);
        assertThat(result.endDate()).isEqualTo(today.plusDays(30));

        ArgumentCaptor<UserSubscription> captor = ArgumentCaptor.forClass(UserSubscription.class);
        verify(subscriptionRepository).save(captor.capture());
        assertThat(captor.getValue().getEndDate()).isEqualTo(today.plusDays(30));
        assertThat(captor.getValue().getDurationDays()).isEqualTo(30);
        verify(appUserRoleRepository).updateRole(userId, "USER_BASIC");
    }

    @Test
    void cancelSubscription_setsStatusCancelledAndDowngradesRole() {
        Long userId = 3L;
        UserSubscription active = UserSubscription.builder()
                .id(3L).userId(userId).planType(SubscriptionPlan.PREMIUM)
                .status(SubscriptionStatus.ACTIVE).startDate(LocalDate.now()).build();
        when(subscriptionRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(active));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserSubscriptionDTO result = service.cancelSubscription(userId);

        assertThat(result.status()).isEqualTo(SubscriptionStatus.CANCELLED);
        verify(appUserRoleRepository).updateRole(userId, "USER_FREE_TRIAL");
    }

    @Test
    void cancelSubscription_noActiveSub_throwsSubscriptionNotFoundException() {
        Long userId = 4L;
        when(subscriptionRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelSubscription(userId))
                .isInstanceOf(SubscriptionNotFoundException.class)
                .hasMessageContaining(userId.toString());
    }

    @Test
    void getCurrentSubscription_expiredSub_updatesStatusAndDowngradesRole() {
        Long userId = 5L;
        UserSubscription expired = UserSubscription.builder()
                .id(5L).userId(userId).planType(SubscriptionPlan.BASIC)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(LocalDate.now().minusDays(31))
                .endDate(LocalDate.now().minusDays(1))
                .build();
        when(subscriptionRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(expired));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserSubscriptionDTO result = service.getCurrentSubscription(userId);

        assertThat(result.status()).isEqualTo(SubscriptionStatus.EXPIRED);
        verify(appUserRoleRepository).updateRole(userId, "USER_FREE_TRIAL");
    }

    @Test
    void getCurrentSubscription_noSub_returnsDefaultFreeTrial() {
        Long userId = 6L;
        when(subscriptionRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());

        UserSubscriptionDTO result = service.getCurrentSubscription(userId);

        assertThat(result.planType()).isEqualTo(SubscriptionPlan.FREE_TRIAL);
        assertThat(result.status()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(result.id()).isNull();
    }
}
