package com.nus.iss.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nus.iss.config.StripeProperties;
import com.nus.iss.dto.SelectPlanRequestDTO;
import com.nus.iss.dto.UserSubscriptionDTO;
import com.nus.iss.entity.PricingPlan;
import com.nus.iss.entity.UserSubscription;
import com.nus.iss.enums.SubscriptionStatus;
import com.nus.iss.exception.SubscriptionNotFoundException;
import com.nus.iss.payment.PaymentResult;
import com.nus.iss.payment.PaymentService;
import com.nus.iss.repository.AppUserRoleRepository;
import com.nus.iss.repository.UserSubscriptionRepository;
import com.nus.iss.service.PricingPlanService;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceImplTest {

    @Mock private UserSubscriptionRepository subscriptionRepository;
    @Mock private AppUserRoleRepository appUserRoleRepository;
    @Mock private PaymentService paymentService;
    @Mock private PricingPlanService pricingPlanService;
    @Mock private StripeProperties stripeProperties;

    private SubscriptionServiceImpl service;

    private static PricingPlan freePlan() {
        return PricingPlan.builder().id(1L).code("FREE_TRIAL").displayName("Free Trial")
                .priceMinor(0L).currency("SGD").billingInterval("MONTHLY").durationDays(null)
                .accessRole("USER_FREE_TRIAL").active(true).displayOrder(1).build();
    }

    private static PricingPlan basicPlan() {
        return PricingPlan.builder().id(2L).code("BASIC").displayName("Basic")
                .priceMinor(990L).currency("SGD").billingInterval("MONTHLY").durationDays(30)
                .accessRole("USER_BASIC").active(true).displayOrder(2).build();
    }

    private static PricingPlan premiumPlan() {
        return PricingPlan.builder().id(3L).code("PREMIUM").displayName("Premium")
                .priceMinor(1990L).currency("SGD").billingInterval("MONTHLY").durationDays(30)
                .accessRole("USER_PREMIUM").active(true).displayOrder(3).build();
    }

    private static UserSubscription stubSub(Long id, Long userId, PricingPlan plan, SubscriptionStatus status) {
        return UserSubscription.builder()
                .id(id).userId(userId).pricingPlan(plan)
                .planCodeSnapshot(plan.getCode()).planNameSnapshot(plan.getDisplayName())
                .priceMinorSnapshot(plan.getPriceMinor()).currencySnapshot(plan.getCurrency())
                .featuresSnapshot("").status(status)
                .durationDays(plan.getDurationDays())
                .startDate(status == SubscriptionStatus.PENDING ? null : LocalDate.now())
                .build();
    }

    @BeforeEach
    void setUp() {
        service = new SubscriptionServiceImpl(subscriptionRepository, appUserRoleRepository,
                paymentService, pricingPlanService, stripeProperties);
    }

    @Test
    void selectPlan_freeTrial_setsNullEndDateAndActivatesImmediately() {
        Long userId = 1L;
        PricingPlan free = freePlan();
        when(subscriptionRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(pricingPlanService.findActive("FREE_TRIAL")).thenReturn(free);
        when(paymentService.processPayment(eq(userId), eq(free), any()))
                .thenReturn(PaymentResult.success("SIM-00000001"));
        UserSubscription saved = stubSub(1L, userId, free, SubscriptionStatus.ACTIVE);
        when(subscriptionRepository.save(any())).thenReturn(saved);

        UserSubscriptionDTO result = service.selectPlan(userId, new SelectPlanRequestDTO("FREE_TRIAL", null));

        assertThat(result.plan().code()).isEqualTo("FREE_TRIAL");
        assertThat(result.status()).isEqualTo(SubscriptionStatus.ACTIVE);

        ArgumentCaptor<UserSubscription> captor = ArgumentCaptor.forClass(UserSubscription.class);
        verify(subscriptionRepository).save(captor.capture());
        assertThat(captor.getValue().getEndDate()).isNull();
        assertThat(captor.getValue().getDurationDays()).isNull();
        verify(appUserRoleRepository).updateRole(userId, "USER_FREE_TRIAL");
    }

    @Test
    void selectPlan_basic_createsPendingSubscriptionWithCheckoutUrl() {
        Long userId = 2L;
        PricingPlan basic = basicPlan();
        when(subscriptionRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(pricingPlanService.findActive("BASIC")).thenReturn(basic);
        when(stripeProperties.paymentLinkFor("BASIC")).thenReturn("https://buy.stripe.com/test_basic");
        UserSubscription saved = stubSub(2L, userId, basic, SubscriptionStatus.PENDING);
        when(subscriptionRepository.save(any())).thenReturn(saved);

        UserSubscriptionDTO result = service.selectPlan(userId, new SelectPlanRequestDTO("BASIC", null));

        assertThat(result.status()).isEqualTo(SubscriptionStatus.PENDING);
        assertThat(result.checkoutUrl()).isEqualTo("https://buy.stripe.com/test_basic?client_reference_id=2");

        ArgumentCaptor<UserSubscription> captor = ArgumentCaptor.forClass(UserSubscription.class);
        verify(subscriptionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(SubscriptionStatus.PENDING);
        assertThat(captor.getValue().getDurationDays()).isEqualTo(30);
        verify(paymentService, never()).processPayment(any(), any(), any());
        verify(appUserRoleRepository, never()).updateRole(any(), any());
    }

    @Test
    void confirmPendingSubscription_activatesAndUpdatesRole() {
        Long userId = 7L;
        PricingPlan premium = premiumPlan();
        LocalDate today = LocalDate.now();
        UserSubscription pending = stubSub(8L, userId, premium, SubscriptionStatus.PENDING);
        when(subscriptionRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, SubscriptionStatus.PENDING))
                .thenReturn(Optional.of(pending));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.confirmPendingSubscription(userId, "cs_test_123");

        ArgumentCaptor<UserSubscription> captor = ArgumentCaptor.forClass(UserSubscription.class);
        verify(subscriptionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(captor.getValue().getStartDate()).isEqualTo(today);
        assertThat(captor.getValue().getEndDate()).isEqualTo(today.plusDays(30));
        assertThat(captor.getValue().getPaymentReference()).isEqualTo("cs_test_123");
        verify(appUserRoleRepository).updateRole(userId, "USER_PREMIUM");
    }

    @Test
    void confirmPendingSubscription_noPendingSub_doesNothing() {
        Long userId = 9L;
        when(subscriptionRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, SubscriptionStatus.PENDING))
                .thenReturn(Optional.empty());

        service.confirmPendingSubscription(userId, "cs_test_456");

        verify(subscriptionRepository, never()).save(any());
        verify(appUserRoleRepository, never()).updateRole(any(), any());
    }

    @Test
    void cancelSubscription_setsStatusCancelledAndDowngradesRole() {
        Long userId = 3L;
        PricingPlan premium = premiumPlan();
        UserSubscription active = stubSub(3L, userId, premium, SubscriptionStatus.ACTIVE);
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
        PricingPlan basic = basicPlan();
        UserSubscription expired = UserSubscription.builder()
                .id(5L).userId(userId).pricingPlan(basic)
                .planCodeSnapshot("BASIC").planNameSnapshot("Basic")
                .priceMinorSnapshot(990L).currencySnapshot("SGD").featuresSnapshot("")
                .status(SubscriptionStatus.ACTIVE)
                .startDate(LocalDate.now().minusDays(31))
                .endDate(LocalDate.now().minusDays(1))
                .durationDays(30)
                .build();
        when(subscriptionRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(expired));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserSubscriptionDTO result = service.getCurrentSubscription(userId);

        assertThat(result.status()).isEqualTo(SubscriptionStatus.EXPIRED);
        verify(appUserRoleRepository).updateRole(userId, "USER_FREE_TRIAL");
    }

    @Test
    void getCurrentSubscription_noActiveButPending_returnsPendingView() {
        Long userId = 10L;
        PricingPlan premium = premiumPlan();
        UserSubscription pending = stubSub(11L, userId, premium, SubscriptionStatus.PENDING);
        when(subscriptionRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(subscriptionRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, SubscriptionStatus.PENDING))
                .thenReturn(Optional.of(pending));

        UserSubscriptionDTO result = service.getCurrentSubscription(userId);

        assertThat(result.status()).isEqualTo(SubscriptionStatus.PENDING);
        assertThat(result.plan().code()).isEqualTo("PREMIUM");
    }

    @Test
    void getCurrentSubscription_noSub_returnsDefaultFreeTrial() {
        Long userId = 6L;
        when(subscriptionRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(subscriptionRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, SubscriptionStatus.PENDING))
                .thenReturn(Optional.empty());
        when(pricingPlanService.findFreePlan()).thenReturn(freePlan());

        UserSubscriptionDTO result = service.getCurrentSubscription(userId);

        assertThat(result.plan().code()).isEqualTo("FREE_TRIAL");
        assertThat(result.status()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(result.id()).isNull();
    }
}
