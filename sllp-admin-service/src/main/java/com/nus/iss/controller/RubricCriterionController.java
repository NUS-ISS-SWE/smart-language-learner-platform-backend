package com.nus.iss.controller;

import com.nus.iss.dto.RubricCriterionRequest;
import com.nus.iss.dto.RubricCriterionResponse;
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

    @PostMapping
    public ResponseEntity<RubricCriterionResponse> create(
            @RequestHeader(USERNAME_HEADER) String username,
            @Valid @RequestBody RubricCriterionRequest request) {
        RubricCriterionResponse created = service.create(request, username);
        return ResponseEntity.created(URI.create(BASE_PATH + "/" + created.id())).body(created);
    }

    @GetMapping
    public ResponseEntity<List<RubricCriterionResponse>> getAll(
            @RequestParam(required = false) Language language,
            @RequestParam(required = false) DifficultyLevel difficultyLevel) {
        return ResponseEntity.ok(service.getAll(language, difficultyLevel));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RubricCriterionResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RubricCriterionResponse> update(
            @PathVariable Long id,
            @RequestHeader(USERNAME_HEADER) String username,
            @Valid @RequestBody RubricCriterionRequest request) {
        return ResponseEntity.ok(service.update(id, request, username));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
