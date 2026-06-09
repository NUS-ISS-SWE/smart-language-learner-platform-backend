package com.nus.iss.dto;

import com.nus.iss.enums.CompositionSource;
import com.nus.iss.enums.CompositionStatus;
import com.nus.iss.enums.Language;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CompositionDTO {
    private Long id;
    private String username;
    private String title;
    private String content;
    private CompositionSource compositionSource;
    private Language language;
    private CompositionStatus compositionStatus;
    private String systemFeedback;
    private String storagePath;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
