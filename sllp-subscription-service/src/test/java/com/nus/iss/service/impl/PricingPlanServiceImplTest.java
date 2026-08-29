package com.nus.iss.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nus.iss.dto.PricingPlanRequest;
import com.nus.iss.entity.PricingPlan;
import com.nus.iss.entity.PricingPlanFeature;
import com.nus.iss.repository.PricingPlanRepository;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PricingPlanServiceImplTest {

    @Mock
    private PricingPlanRepository repository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private PricingPlanServiceImpl service;

    @Test
    void updateFlushesOrphanedFeaturesBeforeAddingReplacementOrders() {
        PricingPlan plan = PricingPlan.builder()
                .id(2L)
                .code("BASIC")
                .active(true)
                .features(new ArrayList<>(List.of(PricingPlanFeature.builder()
                        .featureText("Old feature")
                        .displayOrder(10)
                        .build())))
                .build();
        PricingPlanRequest request = new PricingPlanRequest(null, "Basic", "", 999L,
                "USD", 30, List.of("New feature"), 20);
        when(repository.findByCode("BASIC")).thenReturn(Optional.of(plan));
        doAnswer(invocation -> {
            assertThat(plan.getFeatures()).isEmpty();
            return null;
        }).when(entityManager).flush();

        service.update("BASIC", request);

        verify(entityManager).flush();
        assertThat(plan.getFeatures()).extracting(PricingPlanFeature::getDisplayOrder).containsExactly(10);
    }
}
