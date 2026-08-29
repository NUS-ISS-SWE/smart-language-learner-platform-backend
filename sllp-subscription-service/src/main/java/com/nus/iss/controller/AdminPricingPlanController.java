package com.nus.iss.controller;
import java.util.List;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.nus.iss.dto.*;
import com.nus.iss.service.PricingPlanService;
import lombok.RequiredArgsConstructor;
@RestController @RequestMapping("/api/v1/admin/subscription/plans") @RequiredArgsConstructor
public class AdminPricingPlanController {
 private final PricingPlanService pricingPlanService;
 @GetMapping public List<SubscriptionPlanInfoDTO> list(){ return pricingPlanService.listAll(); }
 @PostMapping public ResponseEntity<SubscriptionPlanInfoDTO> create(@Valid @RequestBody PricingPlanRequest request){ return ResponseEntity.status(HttpStatus.CREATED).body(pricingPlanService.create(request)); }
 @PutMapping("/{code}") public SubscriptionPlanInfoDTO update(@PathVariable String code,@Valid @RequestBody PricingPlanRequest request){ return pricingPlanService.update(code,request); }
 @PatchMapping("/{code}/status") public SubscriptionPlanInfoDTO status(@PathVariable String code,@Valid @RequestBody PricingPlanStatusRequest request){ return pricingPlanService.setStatus(code,request.active()); }
 @PatchMapping("/display-order") @ResponseStatus(HttpStatus.NO_CONTENT) public void order(@Valid @RequestBody PricingPlanOrderRequest request){ pricingPlanService.updateOrder(request); }
}
