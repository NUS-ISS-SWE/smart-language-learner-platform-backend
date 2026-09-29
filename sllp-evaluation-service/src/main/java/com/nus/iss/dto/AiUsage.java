package com.nus.iss.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Provider-reported token counts plus the immutable cost estimate for one request. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiUsage {
    private long promptTokens;
    private long completionTokens;
    private long cachedInputTokens;
    private long totalTokens;
    @JsonIgnore
    private Long estimatedCostMicros;
    private String provider;
    private String model;
    private String pricingVersion;
    private long inputPriceMicrosPerMillion;
    private long cachedInputPriceMicrosPerMillion;
    private long outputPriceMicrosPerMillion;
}
