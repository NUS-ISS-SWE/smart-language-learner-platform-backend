package com.nus.iss.dto;

import com.nus.iss.enums.Language;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CompositionGradingRequestedEvent {
    private UUID requestId;
    private Long compositionId;
    private Long userId;
    private String title;
    private String content;
    private Language language;
    private LocalDateTime requestedAt;
}
