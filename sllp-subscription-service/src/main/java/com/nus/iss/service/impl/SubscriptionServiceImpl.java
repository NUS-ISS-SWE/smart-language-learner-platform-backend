package com.nus.iss.service.impl;

import com.nus.iss.config.StripeProperties;
import com.nus.iss.service.PricingPlanService;
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
import com.nus.iss.service.SubscriptionService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
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

    private final UserSubscriptionRepository subscriptionRepository;
    private final AppUserRoleRepository appUserRoleRepository;
    private final PaymentService paymentService;
    private final PricingPlanService pricingPlanService;
    private final StripeProperties stripeProperties;

    @Override
    @Transactional
    public UserSubscriptionDTO getCurrentSubscription(Long userId) {
        return subscriptionRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, SubscriptionStatus.ACTIVE)
                .map(subscription -> expireIfNeeded(subscription, userId))
                .map(this::toDTO)
                .orElseGet(() -> subscriptionRepository
                        .findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, SubscriptionStatus.PENDING)
                        .map(this::toDTO)
                        .orElseGet(() -> {
                            PricingPlan freePlan = pricingPlanService.findFreePlan();
                            return new UserSubscriptionDTO(null, toSnapshot(freePlan), SubscriptionStatus.ACTIVE,
                                    null, null, null, null, null);
                        }));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSubscriptionDTO> getSubscriptionHistory(Long userId) {
        return subscriptionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public UserSubscriptionDTO selectPlan(Long userId, SelectPlanRequestDTO request) {
        cancelExistingActiveSubscription(userId);

        PricingPlan plan = pricingPlanService.findActive(request.planCode());

        if (plan.getPriceMinor() > 0) {
            return createPendingSubscription(userId, plan);
        }

        PaymentResult result = paymentService.processPayment(userId, plan, request.paymentToken());
        if (!result.success()) {
            throw new InvalidSubscriptionOperationException("Payment failed: " + result.errorMessage());
        }

        LocalDate today = LocalDate.now();
        LocalDate endDate = plan.getDurationDays() != null ? today.plusDays(plan.getDurationDays()) : null;

        UserSubscription newSub = UserSubscription.builder()
                .userId(userId)
                .pricingPlan(plan)
                .planCodeSnapshot(plan.getCode())
                .planNameSnapshot(plan.getDisplayName())
                .priceMinorSnapshot(plan.getPriceMinor())
                .currencySnapshot(plan.getCurrency())
                .featuresSnapshot(serializeFeatures(plan))
                .status(SubscriptionStatus.ACTIVE)
                .startDate(today)
                .endDate(endDate)
                .durationDays(plan.getDurationDays())
                .paymentReference(result.reference())
                .build();

        UserSubscription saved = subscriptionRepository.save(newSub);
        appUserRoleRepository.updateRole(userId, plan.getAccessRole());
        log.info("User {} subscribed to {} — role updated to {}", userId, plan.getCode(), plan.getAccessRole());
        return toDTO(saved);
    }

    private UserSubscriptionDTO createPendingSubscription(Long userId, PricingPlan plan) {
        String paymentLink = stripeProperties.paymentLinkFor(plan.getCode());
        if (paymentLink == null || paymentLink.isBlank()) {
            throw new InvalidSubscriptionOperationException(
                    "No Stripe Payment Link configured for plan " + plan.getCode());
        }

        UserSubscription pending = UserSubscription.builder()
                .userId(userId)
                .pricingPlan(plan)
                .planCodeSnapshot(plan.getCode())
                .planNameSnapshot(plan.getDisplayName())
                .priceMinorSnapshot(plan.getPriceMinor())
                .currencySnapshot(plan.getCurrency())
                .featuresSnapshot(serializeFeatures(plan))
                .status(SubscriptionStatus.PENDING)
                .durationDays(plan.getDurationDays())
                .build();

        UserSubscription saved = subscriptionRepository.save(pending);
        log.info("Created PENDING subscription {} for user {} plan {} — awaiting Stripe payment",
                saved.getId(), userId, plan.getCode());

        String checkoutUrl = paymentLink + "?client_reference_id=" + userId;
        return toDTO(saved, checkoutUrl);
    }

    @Override
    @Transactional
    public void confirmPendingSubscription(Long userId, String stripeReference) {
        Optional<UserSubscription> pendingOpt = subscriptionRepository
                .findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, SubscriptionStatus.PENDING);

        if (pendingOpt.isEmpty()) {
            log.warn("Stripe confirmed payment for user {} but no PENDING subscription was found "
                    + "(already processed, or webhook delivered twice) — ignoring", userId);
            return;
        }

        UserSubscription sub = pendingOpt.get();
        LocalDate today = LocalDate.now();
        LocalDate endDate = sub.getDurationDays() != null ? today.plusDays(sub.getDurationDays()) : null;

        sub.setStatus(SubscriptionStatus.ACTIVE);
        sub.setStartDate(today);
        sub.setEndDate(endDate);
        sub.setPaymentReference(stripeReference);
        subscriptionRepository.save(sub);

        String accessRole = sub.getPricingPlan().getAccessRole();
        appUserRoleRepository.updateRole(userId, accessRole);
        log.info("Stripe payment confirmed for user {} — subscription {} activated for plan {}",
                userId, sub.getId(), sub.getPlanCodeSnapshot());
    }

    @Override
    @Transactional
    public UserSubscriptionDTO cancelSubscription(Long userId) {
        UserSubscription subscription = findActiveSubscription(userId);
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        UserSubscription saved = subscriptionRepository.save(subscription);
        downgradeToFreeTrial(userId, saved.getId());
        return toDTO(saved);
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
                    log.info("Cancelled previous subscription {} for user {}", subscription.getId(), userId);
                });
    }

    private UserSubscription findActiveSubscription(Long userId) {
        return subscriptionRepository.findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new SubscriptionNotFoundException("No active subscription found for user " + userId));
    }

    private void downgradeToFreeTrial(Long userId, Long subscriptionId) {
        appUserRoleRepository.updateRole(userId, FREE_TRIAL_ROLE);
        log.info("Subscription {} for user {} downgraded to FREE_TRIAL", subscriptionId, userId);
    }

    private String serializeFeatures(PricingPlan plan) {
        return plan.getFeatures().stream()
                .map(PricingPlanFeature::getFeatureText)
                .collect(Collectors.joining("\n"));
    }

    private SubscriptionPlanSnapshotDTO toSnapshot(UserSubscription sub) {
        List<String> features = sub.getFeaturesSnapshot() == null || sub.getFeaturesSnapshot().isBlank()
                ? List.of()
                : List.of(sub.getFeaturesSnapshot().split("\n"));
        return new SubscriptionPlanSnapshotDTO(
                sub.getPlanCodeSnapshot(),
                sub.getPlanNameSnapshot(),
                sub.getPriceMinorSnapshot(),
                sub.getCurrencySnapshot(),
                features
        );
    }

    private SubscriptionPlanSnapshotDTO toSnapshot(PricingPlan plan) {
        return new SubscriptionPlanSnapshotDTO(
                plan.getCode(),
                plan.getDisplayName(),
                plan.getPriceMinor(),
                plan.getCurrency(),
                plan.getFeatures().stream().map(PricingPlanFeature::getFeatureText).toList()
        );
    }

    private UserSubscriptionDTO toDTO(UserSubscription sub) {
        return toDTO(sub, null);
    }

    private UserSubscriptionDTO toDTO(UserSubscription sub, String checkoutUrl) {
        return new UserSubscriptionDTO(
                sub.getId(),
                toSnapshot(sub),
                sub.getStatus(),
                sub.getStartDate(),
                sub.getEndDate(),
                sub.getDurationDays(),
                sub.getCreatedAt(),
                checkoutUrl
        );
    }
}
