package com.nus.iss.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nus.iss.config.SllpConstant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class A2aResponseDTO {
    private String jsonrpc;
    private String id;
    private Result result;
    private RpcError error;

    public static A2aResponseDTO completed(String requestId, String contextId, GradingResultDTO grading) {
        Part part = Part.builder().mediaType(SllpConstant.JSON_MEDIA_TYPE).data(grading).build();
        Artifact artifact = Artifact.builder().artifactId(grading.getArtifactId()).parts(List.of(part)).build();
        Task task = Task.builder()
                .id(grading.getTaskId())
                .contextId(contextId)
                .status(new TaskStatus(SllpConstant.COMPLETED_STATE))
                .artifacts(List.of(artifact))
                .build();
        return A2aResponseDTO.builder().jsonrpc(SllpConstant.JSON_RPC_VERSION).id(requestId)
                .result(new Result(task)).build();
    }

    public static A2aResponseDTO error(String requestId, int code, String message) {
        return A2aResponseDTO.builder().jsonrpc(SllpConstant.JSON_RPC_VERSION).id(requestId)
                .error(new RpcError(code, message)).build();
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Result { private Task task; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Task {
        private String id;
        private String contextId;
        private TaskStatus status;
        private List<Artifact> artifacts;
    }
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class TaskStatus { private String state; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Artifact { private String artifactId; private List<Part> parts; }
    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Part { private String mediaType; private GradingResultDTO data; }
    @Data @NoArgsConstructor @AllArgsConstructor
    public static class RpcError { private int code; private String message; }
}
