package com.nus.iss.dto;
import java.util.List;
public record SubscriptionPlanSnapshotDTO(String code, String displayName, long priceMinor, String currency, List<String> features) {}
