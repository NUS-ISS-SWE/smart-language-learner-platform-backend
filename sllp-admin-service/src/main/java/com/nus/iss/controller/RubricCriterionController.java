package com.nus.iss.controller;

import com.nus.iss.dto.RubricCriterionRequest;
import com.nus.iss.dto.RubricCriterionResponse;
import com.nus.iss.audit.AdminAuditRecorder;
import com.nus.iss.enums.DifficultyLevel;
import com.nus.iss.enums.Language;
import com.nus.iss.service.RubricCriterionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping(RubricCriterionController.BASE_PATH)
@RequiredArgsConstructor
public class RubricCriterionController {

    static final String BASE_PATH = "/api/v1/admin/rubrics";
    private static final String USERNAME_HEADER = "X-Sllp-Username";

    private final RubricCriterionService service;
    private final AdminAuditRecorder audit;

    @PostMapping
    public ResponseEntity<RubricCriterionResponse> create(
            @RequestHeader(USERNAME_HEADER) String username,
            @RequestHeader(value = "X-Sllp-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Sllp-Request-Id", required = false) String requestId,
            @Valid @RequestBody RubricCriterionRequest request) {
        RubricCriterionResponse created = service.create(request, username);
        audit.record("RUBRIC_CREATED", username, userId, "RUBRIC_CRITERION", created.id().toString(), requestId, "POST", BASE_PATH);
        return ResponseEntity.created(URI.create(BASE_PATH + "/" + created.id())).body(created);
    }

    @GetMapping
    public ResponseEntity<List<RubricCriterionResponse>> getAll(
            @RequestHeader(value = USERNAME_HEADER, required = false) String username,
            @RequestHeader(value = "X-Sllp-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Sllp-Request-Id", required = false) String requestId,
            @RequestParam(required = false) Language language,
            @RequestParam(required = false) DifficultyLevel difficultyLevel) {
        audit.record("RUBRIC_LIST_VIEWED", username, userId, "RUBRIC_CRITERION", null, requestId, "GET", BASE_PATH);
        return ResponseEntity.ok(service.getAll(language, difficultyLevel));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RubricCriterionResponse> getById(@PathVariable Long id,
            @RequestHeader(value = USERNAME_HEADER, required = false) String username,
            @RequestHeader(value = "X-Sllp-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Sllp-Request-Id", required = false) String requestId) {
        audit.record("RUBRIC_VIEWED", username, userId, "RUBRIC_CRITERION", id.toString(), requestId, "GET", BASE_PATH + "/" + id);
        return ResponseEntity.ok(service.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RubricCriterionResponse> update(
            @PathVariable Long id,
            @RequestHeader(USERNAME_HEADER) String username,
            @RequestHeader(value = "X-Sllp-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Sllp-Request-Id", required = false) String requestId,
            @Valid @RequestBody RubricCriterionRequest request) {
        RubricCriterionResponse updated = service.update(id, request, username);
        audit.record("RUBRIC_UPDATED", username, userId, "RUBRIC_CRITERION", id.toString(), requestId, "PUT", BASE_PATH + "/" + id);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
            @RequestHeader(value = USERNAME_HEADER, required = false) String username,
            @RequestHeader(value = "X-Sllp-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Sllp-Request-Id", required = false) String requestId) {
        service.delete(id);
        audit.record("RUBRIC_DELETED", username, userId, "RUBRIC_CRITERION", id.toString(), requestId, "DELETE", BASE_PATH + "/" + id);
        return ResponseEntity.noContent().build();
    }
}
