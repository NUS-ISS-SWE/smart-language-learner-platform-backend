package com.nus.iss.dto;

import com.nus.iss.enums.Language;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradingRequestDTO {
    private String requestId;
    private String contextId;
    private Long compositionId;
    private Long userId;
    private String title;
    private String content;
    private Language language;
    private List<String> evidence;
    private String requestHash;
}
