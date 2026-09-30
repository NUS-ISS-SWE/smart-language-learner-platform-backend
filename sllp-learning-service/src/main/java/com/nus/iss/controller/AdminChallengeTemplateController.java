package com.nus.iss.controller;

import com.nus.iss.dto.ChallengeTemplateRequest;
import com.nus.iss.dto.ChallengeTemplateResponse;
import com.nus.iss.service.LearningProgressService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

/** Gateway restricts this namespace to ADMIN users. */
@RestController
@RequestMapping("/api/v1/admin/learning/challenges")
public class AdminChallengeTemplateController {
    private final LearningProgressService service;
    public AdminChallengeTemplateController(LearningProgressService service) { this.service = service; }
    @GetMapping public List<ChallengeTemplateResponse> list() { return service.listTemplates(); }
    @PostMapping public ResponseEntity<ChallengeTemplateResponse> create(@Valid @RequestBody ChallengeTemplateRequest request) {
        ChallengeTemplateResponse created = service.createTemplate(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/learning/challenges/" + created.id())).body(created);
    }
    @PutMapping("/{id}") public ChallengeTemplateResponse update(@PathVariable Long id, @Valid @RequestBody ChallengeTemplateRequest request) { return service.updateTemplate(id, request); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id) { service.deleteTemplate(id); return ResponseEntity.noContent().build(); }
}
