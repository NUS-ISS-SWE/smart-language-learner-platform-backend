package com.nus.iss.service.impl;

import com.nus.iss.dto.PricingPlanOrderRequest;
import com.nus.iss.dto.PricingPlanRequest;
import com.nus.iss.dto.SubscriptionPlanInfoDTO;
import com.nus.iss.entity.PricingPlan;
import com.nus.iss.entity.PricingPlanFeature;
import com.nus.iss.exception.InvalidSubscriptionOperationException;
import com.nus.iss.exception.PricingPlanNotFoundException;
import com.nus.iss.repository.PricingPlanRepository;
import com.nus.iss.service.PricingPlanService;
import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PricingPlanServiceImpl implements PricingPlanService {

    private static final String MONTHLY = "MONTHLY";
    private static final String FREE_TRIAL_CODE = "FREE_TRIAL";
    private static final String FREE_TRIAL_ROLE = "USER_FREE_TRIAL";
    private static final String PAID_PLAN_ROLE = "USER_BASIC";
    private static final int FEATURE_ORDER_STEP = 10;

    private final PricingPlanRepository repository;
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionPlanInfoDTO> listActive() {
        return repository.findByActiveTrueOrderByDisplayOrderAsc().stream().map(this::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionPlanInfoDTO> listAll() {
        return repository.findAllByOrderByDisplayOrderAsc().stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public SubscriptionPlanInfoDTO create(PricingPlanRequest request) {
        validateCreationRequest(request);
        PricingPlan plan = PricingPlan.builder().code(request.code()).active(true).build();
        applyEditableFields(plan, request);
        return toDto(repository.save(plan));
    }

    @Override
    @Transactional
    public SubscriptionPlanInfoDTO update(String code, PricingPlanRequest request) {
        if (request.code() != null && !code.equals(request.code())) {
            throw new InvalidSubscriptionOperationException("Plan code cannot be changed");
        }
        PricingPlan plan = findByCode(code);
        applyEditableFields(plan, request);
        return toDto(plan);
    }

    @Override
    @Transactional
    public SubscriptionPlanInfoDTO setStatus(String code, boolean active) {
        PricingPlan plan = findByCode(code);
        plan.setActive(active);
        return toDto(plan);
    }

    @Override
    @Transactional
    public void updateOrder(PricingPlanOrderRequest request) {
        request.items().forEach(item -> findByCode(item.code()).setDisplayOrder(item.displayOrder()));
    }

    @Override
    @Transactional(readOnly = true)
    public PricingPlan findActive(String code) {
        return repository.findByCodeAndActiveTrue(code)
                .orElseThrow(() -> new PricingPlanNotFoundException("Active pricing plan not found: " + code));
    }

    @Override
    @Transactional(readOnly = true)
    public PricingPlan findFreePlan() {
        return findActive(FREE_TRIAL_CODE);
    }

    private void validateCreationRequest(PricingPlanRequest request) {
        if (request.code() == null) {
            throw new InvalidSubscriptionOperationException("code is required");
        }
        if (repository.findByCode(request.code()).isPresent()) {
            throw new InvalidSubscriptionOperationException("A plan with this code already exists");
        }
    }

    private PricingPlan findByCode(String code) {
        return repository.findByCode(code)
                .orElseThrow(() -> new PricingPlanNotFoundException("Pricing plan not found: " + code));
    }

    private void applyEditableFields(PricingPlan plan, PricingPlanRequest request) {
        validateCommercialTerms(request);
        plan.setDisplayName(request.displayName().trim());
        plan.setDescription(blankToNull(request.description()));
        plan.setPriceMinor(request.priceMinor());
        plan.setCurrency(request.currency());
        plan.setBillingInterval(MONTHLY);
        plan.setDurationDays(request.priceMinor() == 0 ? null : request.durationDays());
        plan.setAccessRole(request.priceMinor() == 0 ? FREE_TRIAL_ROLE : PAID_PLAN_ROLE);
        plan.setDisplayOrder(request.displayOrder());
        replaceFeatures(plan, request.features());
    }

    private void validateCommercialTerms(PricingPlanRequest request) {
        if (request.priceMinor() > 0 && request.durationDays() == null) {
            throw new InvalidSubscriptionOperationException("Paid monthly plans require a durationDays value");
        }
    }

    private void replaceFeatures(PricingPlan plan, List<String> features) {
        plan.getFeatures().clear();
        // Hibernate otherwise inserts replacement rows before orphan-removal
        // deletes, conflicting with the unique plan/order constraint.
        if (plan.getId() != null) {
            entityManager.flush();
        }
        for (int index = 0; index < features.size(); index++) {
            plan.getFeatures().add(PricingPlanFeature.builder()
                    .pricingPlan(plan)
                    .featureText(features.get(index).trim())
                    .displayOrder((index + 1) * FEATURE_ORDER_STEP)
                    .build());
        }
    }

    private SubscriptionPlanInfoDTO toDto(PricingPlan plan) {
        return new SubscriptionPlanInfoDTO(plan.getCode(), plan.getDisplayName(), plan.getDescription(),
                plan.getPriceMinor(), plan.getCurrency(), plan.getBillingInterval(), plan.getDurationDays(),
                plan.getFeatures().stream().map(PricingPlanFeature::getFeatureText).toList(),
                plan.isActive(), plan.getDisplayOrder());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
