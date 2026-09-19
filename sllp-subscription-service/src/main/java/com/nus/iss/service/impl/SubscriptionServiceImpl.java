package com.nus.iss.service.impl;

import com.nus.iss.dto.SelectPlanRequestDTO;
import com.nus.iss.dto.SubscriptionPlanInfoDTO;
import com.nus.iss.dto.SubscriptionPlanSnapshotDTO;
import com.nus.iss.dto.UserSubscriptionDTO;
import com.nus.iss.entity.PricingPlan;
import com.nus.iss.entity.PricingPlanFeature;
import com.nus.iss.entity.UserSubscription;
import com.nus.iss.enums.SubscriptionStatus;
import com.nus.iss.exception.InvalidSubscriptionOperationException;
import com.nus.iss.exception.SubscriptionNotFoundException;
import com.nus.iss.payment.PaymentResult;
import com.nus.iss.payment.PaymentService;
import com.nus.iss.repository.AppUserRoleRepository;
import com.nus.iss.repository.UserSubscriptionRepository;
import com.nus.iss.service.PricingPlanService;
import com.nus.iss.service.SubscriptionService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private static final String FREE_TRIAL_ROLE = "USER_FREE_TRIAL";
    private static final ObjectMapper SNAPSHOT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> FEATURE_LIST_TYPE = new TypeReference<>() {};

    private final UserSubscriptionRepository subscriptionRepository;
    private final AppUserRoleRepository appUserRoleRepository;
    private final PaymentService paymentService;
    private final PricingPlanService pricingPlanService;

    @Override
    @Transactional
    public UserSubscriptionDTO getCurrentSubscription(Long userId) {
        return subscriptionRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, SubscriptionStatus.ACTIVE)
                .map(subscription -> expireIfNeeded(subscription, userId))
                .map(this::toDto)
                .orElseGet(() -> defaultFreeTrialSubscription());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSubscriptionDTO> getSubscriptionHistory(Long userId) {
        return subscriptionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public UserSubscriptionDTO selectPlan(Long userId, SelectPlanRequestDTO request) {
        PricingPlan plan = pricingPlanService.findActive(request.planCode());
        PaymentResult payment = paymentService.processPayment(userId, plan, request.paymentToken());
        if (!payment.success()) {
            throw new InvalidSubscriptionOperationException("Payment failed: " + payment.errorMessage());
        }

        cancelExistingActiveSubscription(userId);
        UserSubscription subscription = subscriptionRepository.save(newSubscription(userId, plan, payment.reference()));
        appUserRoleRepository.updateRole(userId, plan.getAccessRole());
        return toDto(subscription);
    }

    @Override
    @Transactional
    public UserSubscriptionDTO cancelSubscription(Long userId) {
        UserSubscription subscription = findActiveSubscription(userId);
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        UserSubscription saved = subscriptionRepository.save(subscription);
        downgradeToFreeTrial(userId, saved.getId());
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionPlanInfoDTO> getAvailablePlans() {
        return pricingPlanService.listActive();
    }

    private UserSubscription expireIfNeeded(UserSubscription subscription, Long userId) {
        if (subscription.getEndDate() == null || !subscription.getEndDate().isBefore(LocalDate.now())) {
            return subscription;
        }
        subscription.setStatus(SubscriptionStatus.EXPIRED);
        UserSubscription expired = subscriptionRepository.save(subscription);
        downgradeToFreeTrial(userId, expired.getId());
        return expired;
    }

    private void cancelExistingActiveSubscription(Long userId) {
        subscriptionRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, SubscriptionStatus.ACTIVE)
                .ifPresent(subscription -> {
                    subscription.setStatus(SubscriptionStatus.CANCELLED);
                    subscriptionRepository.save(subscription);
                });
    }

    private UserSubscription findActiveSubscription(Long userId) {
        return subscriptionRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new SubscriptionNotFoundException("No active subscription found for user " + userId));
    }

    private UserSubscription newSubscription(Long userId, PricingPlan plan, String paymentReference) {
        LocalDate startDate = LocalDate.now();
        return UserSubscription.builder()
                .userId(userId)
                .pricingPlan(plan)
                .planCodeSnapshot(plan.getCode())
                .planNameSnapshot(plan.getDisplayName())
                .priceMinorSnapshot(plan.getPriceMinor())
                .currencySnapshot(plan.getCurrency())
                .featuresSnapshot(serializeFeatures(plan))
                .status(SubscriptionStatus.ACTIVE)
                .startDate(startDate)
                .endDate(plan.getDurationDays() == null ? null : startDate.plusDays(plan.getDurationDays()))
                .durationDays(plan.getDurationDays())
                .paymentReference(paymentReference)
                .build();
    }

    private UserSubscriptionDTO defaultFreeTrialSubscription() {
        return new UserSubscriptionDTO(null, snapshotOf(pricingPlanService.findFreePlan()),
                SubscriptionStatus.ACTIVE, null, null, null, null);
    }

    private void downgradeToFreeTrial(Long userId, Long subscriptionId) {
        appUserRoleRepository.updateRole(userId, FREE_TRIAL_ROLE);
        log.info("Subscription {} for user {} downgraded to FREE_TRIAL", subscriptionId, userId);
    }

    private UserSubscriptionDTO toDto(UserSubscription subscription) {
        return new UserSubscriptionDTO(subscription.getId(),
                new SubscriptionPlanSnapshotDTO(subscription.getPlanCodeSnapshot(), subscription.getPlanNameSnapshot(),
                        subscription.getPriceMinorSnapshot(), subscription.getCurrencySnapshot(),
                        deserializeFeatures(subscription.getFeaturesSnapshot())),
                subscription.getStatus(), subscription.getStartDate(), subscription.getEndDate(),
                subscription.getDurationDays(), subscription.getCreatedAt());
    }

    private SubscriptionPlanSnapshotDTO snapshotOf(PricingPlan plan) {
        return new SubscriptionPlanSnapshotDTO(plan.getCode(), plan.getDisplayName(), plan.getPriceMinor(),
                plan.getCurrency(), featureTexts(plan));
    }

    private String serializeFeatures(PricingPlan plan) {
        try {
            return SNAPSHOT_MAPPER.writeValueAsString(featureTexts(plan));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not snapshot pricing-plan features", exception);
        }
    }

    private List<String> deserializeFeatures(String serializedFeatures) {
        try {
            return SNAPSHOT_MAPPER.readValue(serializedFeatures, FEATURE_LIST_TYPE);
        } catch (Exception exception) {
            log.warn("Could not read subscription feature snapshot; returning an empty feature list", exception);
            return List.of();
        }
    }

    private List<String> featureTexts(PricingPlan plan) {
        return plan.getFeatures().stream().map(PricingPlanFeature::getFeatureText).toList();
    }
}
