package com.nus.iss.controller;

import com.nus.iss.dto.AchievementDefinitionRequest;
import com.nus.iss.dto.AchievementDefinitionResponse;
import com.nus.iss.service.LearningProgressService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/** Gateway restricts this namespace to ADMIN users. Definitions are deactivated rather than deleted to preserve earned badges. */
@RestController
@RequestMapping("/api/v1/admin/learning/achievements")
public class AdminAchievementDefinitionController {
    private final LearningProgressService service;
    public AdminAchievementDefinitionController(LearningProgressService service) { this.service = service; }
    @GetMapping public List<AchievementDefinitionResponse> list() { return service.listAchievementDefinitions(); }
    @PostMapping public ResponseEntity<AchievementDefinitionResponse> create(@Valid @RequestBody AchievementDefinitionRequest request) {
        AchievementDefinitionResponse created = service.createAchievementDefinition(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/learning/achievements/" + created.id())).body(created);
    }
    @PutMapping("/{id}") public AchievementDefinitionResponse update(@PathVariable Long id, @Valid @RequestBody AchievementDefinitionRequest request) { return service.updateAchievementDefinition(id, request); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> deactivate(@PathVariable Long id) { service.deactivateAchievementDefinition(id); return ResponseEntity.noContent().build(); }
}
