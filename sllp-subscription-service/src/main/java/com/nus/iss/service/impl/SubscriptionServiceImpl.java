package com.nus.iss.service.impl;

import com.nus.iss.catalog.SubscriptionPlanCatalog;
import com.nus.iss.dto.SelectPlanRequestDTO;
import com.nus.iss.dto.SubscriptionPlanInfoDTO;
import com.nus.iss.dto.UserSubscriptionDTO;
import com.nus.iss.entity.UserSubscription;
import com.nus.iss.enums.SubscriptionPlan;
import com.nus.iss.enums.SubscriptionStatus;
import com.nus.iss.exception.InvalidSubscriptionOperationException;
import com.nus.iss.exception.SubscriptionNotFoundException;
import com.nus.iss.payment.PaymentResult;
import com.nus.iss.payment.PaymentService;
import com.nus.iss.repository.AppUserRoleRepository;
import com.nus.iss.repository.UserSubscriptionRepository;
import com.nus.iss.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final UserSubscriptionRepository subscriptionRepository;
    private final AppUserRoleRepository appUserRoleRepository;
    private final PaymentService paymentService;
    private final SubscriptionPlanCatalog planCatalog;

    @Override
    @Transactional
    public UserSubscriptionDTO getCurrentSubscription(Long userId) {
        Optional<UserSubscription> activeOpt = subscriptionRepository
                .findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, SubscriptionStatus.ACTIVE);

        if (activeOpt.isEmpty()) {
            // No subscription record — return a default FREE_TRIAL view (not persisted)
            return new UserSubscriptionDTO(null, SubscriptionPlan.FREE_TRIAL, SubscriptionStatus.ACTIVE,
                    null, null, null, null);
        }

        UserSubscription sub = activeOpt.get();

        // On-demand expiry check: if end_date is in the past, expire the subscription
        if (sub.getEndDate() != null && sub.getEndDate().isBefore(LocalDate.now())) {
            sub.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(sub);
            downgradeToFreeTrial(sub.getUserId(), sub.getId());
        }

        return toDTO(sub);
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
        // Cancel any currently active subscription before switching plans
        subscriptionRepository
                .findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, SubscriptionStatus.ACTIVE)
                .ifPresent(existing -> {
                    existing.setStatus(SubscriptionStatus.CANCELLED);
                    subscriptionRepository.save(existing);
                    log.info("Cancelled previous subscription {} for user {}", existing.getId(), userId);
                });

        // Delegate to PaymentService — simulated by default; Stripe dev wires StripePaymentServiceImpl
        PaymentResult result = paymentService.processPayment(userId, request.plan(), request.paymentToken());
        if (!result.success()) {
            throw new InvalidSubscriptionOperationException("Payment failed: " + result.errorMessage());
        }

        LocalDate today = LocalDate.now();
        LocalDate endDate = request.plan().durationDays() != null
                ? today.plusDays(request.plan().durationDays())
                : null;

        UserSubscription newSub = UserSubscription.builder()
                .userId(userId)
                .planType(request.plan())
                .status(SubscriptionStatus.ACTIVE)
                .startDate(today)
                .endDate(endDate)
                .durationDays(request.plan().durationDays())
                .paymentReference(result.reference())
                .build();

        UserSubscription saved = subscriptionRepository.save(newSub);

        appUserRoleRepository.updateRole(userId, request.plan().roleValue());
        log.info("User {} subscribed to {} — role updated to {}", userId, request.plan(), request.plan().roleValue());

        return toDTO(saved);
    }

    @Override
    @Transactional
    public UserSubscriptionDTO cancelSubscription(Long userId) {
        UserSubscription sub = subscriptionRepository
                .findFirstByUserIdAndStatusOrderByCreatedAtDesc(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new SubscriptionNotFoundException(
                        "No active subscription found for user " + userId));

        sub.setStatus(SubscriptionStatus.CANCELLED);
        UserSubscription saved = subscriptionRepository.save(sub);

        downgradeToFreeTrial(userId, sub.getId());

        return toDTO(saved);
    }

    @Override
    public List<SubscriptionPlanInfoDTO> getAvailablePlans() {
        return planCatalog.getAll();
    }

    private void downgradeToFreeTrial(Long userId, Long subscriptionId) {
        appUserRoleRepository.updateRole(userId, SubscriptionPlan.FREE_TRIAL.roleValue());
        log.info("Subscription {} for user {} downgraded to {} — role set to {}",
                subscriptionId, userId, SubscriptionPlan.FREE_TRIAL, SubscriptionPlan.FREE_TRIAL.roleValue());
    }

    private UserSubscriptionDTO toDTO(UserSubscription sub) {
        return new UserSubscriptionDTO(
                sub.getId(),
                sub.getPlanType(),
                sub.getStatus(),
                sub.getStartDate(),
                sub.getEndDate(),
                sub.getDurationDays(),
                sub.getCreatedAt()
        );
    }
}
