package com.nus.iss.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.Optional;
import com.nus.iss.dto.SelectPlanRequestDTO;
import com.nus.iss.entity.*;
import com.nus.iss.enums.SubscriptionStatus;
import com.nus.iss.payment.*;
import com.nus.iss.repository.*;
import com.nus.iss.service.PricingPlanService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceImplTest {
 @Mock UserSubscriptionRepository subscriptions; @Mock AppUserRoleRepository roles; @Mock PaymentService payment; @Mock PricingPlanService plans;
 SubscriptionServiceImpl service;
 @BeforeEach void setUp(){service=new SubscriptionServiceImpl(subscriptions,roles,payment,plans);}
 @Test void selectingAnActivePlanSnapshotsItsTermsAndUsesPaidRole(){
  PricingPlan plan=PricingPlan.builder().id(3L).code("PRO").displayName("Pro").priceMinor(1999L).currency("USD").durationDays(30).accessRole("USER_BASIC").features(List.of(PricingPlanFeature.builder().featureText("Priority support").displayOrder(10).build())).build();
  when(plans.findActive("PRO")).thenReturn(plan); when(payment.processPayment(7L,plan,null)).thenReturn(PaymentResult.success("SIM-1")); when(subscriptions.findFirstByUserIdAndStatusOrderByCreatedAtDesc(7L,SubscriptionStatus.ACTIVE)).thenReturn(Optional.empty()); when(subscriptions.save(any())).thenAnswer(i->i.getArgument(0));
  var result=service.selectPlan(7L,new SelectPlanRequestDTO("PRO",null));
  assertThat(result.plan().code()).isEqualTo("PRO"); assertThat(result.plan().priceMinor()).isEqualTo(1999L); verify(roles).updateRole(7L,"USER_BASIC");
 }
}
