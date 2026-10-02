package com.nus.iss.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class A2aRequestDTO {
    private String jsonrpc;
    private String id;
    private String method;
    private Params params;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Params {
        private Message message;
        private Object configuration;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Message {
        private String messageId;
        private String contextId;
        private String role;
        private List<Part> parts;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Part {
        private String mediaType;
        private GradingData data;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GradingData {
        private String schemaVersion;
        private String workflowId;
        private String correlationId;
        private Integer attempt;
        private Composition composition;
        private EvidenceArtifact evidence;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Composition {
        private Long compositionId;
        private Long userId;
        private String title;
        private String content;
        private String language;
        private String requestedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EvidenceArtifact {
        private List<EvidenceCriterion> criteria;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EvidenceCriterion {
        private String criterion;
        private List<EvidenceItem> evidence;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EvidenceItem {
        private String excerpt;
        private String polarity;
        private String note;
        private Boolean verifiedOnFirstPass;
    }
}
