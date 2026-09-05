package com.nus.iss.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Immutable provider-reported AI usage and the price snapshot applied to it. */
@Entity
@Table(name = "ai_usage_event")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiUsageEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, unique = true)
    private UUID requestId;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "composition_id")
    private Long compositionId;
    @Column(nullable = false)
    private String operation;
    @Column(nullable = false)
    private String provider;
    @Column(nullable = false)
    private String model;
    @Column(name = "prompt_tokens", nullable = false)
    private long promptTokens;
    @Column(name = "completion_tokens", nullable = false)
    private long completionTokens;
    @Column(name = "cached_input_tokens", nullable = false)
    private long cachedInputTokens;
    @Column(name = "total_tokens", nullable = false)
    private long totalTokens;
    @Column(name = "estimated_cost_micros", nullable = false)
    private long estimatedCostMicros;
    @Column(nullable = false)
    private String currency;
    @Column(name = "pricing_version", nullable = false)
    private String pricingVersion;
    @Column(name = "input_price_micros_per_million", nullable = false)
    private long inputPriceMicrosPerMillion;
    @Column(name = "cached_input_price_micros_per_million", nullable = false)
    private long cachedInputPriceMicrosPerMillion;
    @Column(name = "output_price_micros_per_million", nullable = false)
    private long outputPriceMicrosPerMillion;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
