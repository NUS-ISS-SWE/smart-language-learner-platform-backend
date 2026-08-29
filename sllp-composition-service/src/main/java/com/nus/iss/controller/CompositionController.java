package com.nus.iss.controller;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

import com.nus.iss.dto.CompositionExampleResponse;
import com.nus.iss.dto.CompositionResponse;
import com.nus.iss.dto.CreateCompositionRequest;
import com.nus.iss.dto.GenerateCompositionRequest;
import com.nus.iss.dto.UpdateCompositionRequest;
import com.nus.iss.enums.Language;
import com.nus.iss.service.CompositionCommandService;
import com.nus.iss.service.CompositionExampleService;
import com.nus.iss.service.CompositionQueryService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** HTTP-only adapter for composition commands and queries. */
@RestController
@RequestMapping("/api/v1")
public class CompositionController {
    private final CompositionCommandService commandService;
    private final CompositionQueryService queryService;
    private final CompositionExampleService exampleService;

    public CompositionController(CompositionCommandService commandService, CompositionQueryService queryService,
                                 CompositionExampleService exampleService) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.exampleService = exampleService;
    }

    @PostMapping("/composition")
    public ResponseEntity<CompositionResponse> createNewComposition(@RequestHeader("X-Sllp-User-Id") Long userId,
                                                                      @RequestBody @Valid @NotNull CreateCompositionRequest request) {
        return ResponseEntity.ok(commandService.create(userId, request));
    }

    @GetMapping("/composition")
    public ResponseEntity<List<CompositionResponse>> getAllCompositionByUserId(@RequestHeader("X-Sllp-User-Id") Long userId,
                                                                                 @RequestParam(defaultValue = "0") int page,
                                                                                 @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(queryService.listForUser(userId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @GetMapping("/composition/{id}")
    public ResponseEntity<CompositionResponse> getSingleCompositionById(@RequestHeader("X-Sllp-User-Id") Long userId,
                                                                          @PathVariable Long id) {
        return ResponseEntity.ok(queryService.getForUser(userId, id));
    }

    @PutMapping("/composition/{id}")
    public ResponseEntity<CompositionResponse> updateComposition(@RequestHeader("X-Sllp-User-Id") Long userId,
                                                                   @PathVariable Long id,
                                                                   @RequestBody @Valid @NotNull UpdateCompositionRequest request) {
        return ResponseEntity.ok(commandService.update(userId, id, request));
    }

    @DeleteMapping("/composition/{id}")
    public ResponseEntity<Void> deleteComposition(@RequestHeader("X-Sllp-User-Id") Long userId, @PathVariable Long id) {
        commandService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/composition/{id}/grading")
    public ResponseEntity<CompositionResponse> requestToGradeComposition(@RequestHeader("X-Sllp-User-Id") Long userId,
                                                                            @PathVariable Long id) {
        return ResponseEntity.ok(commandService.requestGrading(userId, id));
    }

    @PostMapping("/composition/generate")
    public ResponseEntity<CompositionResponse> generateComposition(@RequestHeader("X-Sllp-User-Id") Long userId,
                                                                      @RequestBody @Valid @NotNull GenerateCompositionRequest request) {
        return ResponseEntity.ok(commandService.generate(userId, request));
    }

    @GetMapping("/composition/examples")
    public ResponseEntity<List<CompositionExampleResponse>> getAllCompositionExamples() {
        return ResponseEntity.ok(exampleService.getAllExamples());
    }

    @GetMapping("/composition/examples/language/{language}")
    public ResponseEntity<List<CompositionExampleResponse>> getCompositionExamplesByLanguage(
            @PathVariable String language) {
        try {
            Language lang = Language.valueOf(language.toUpperCase());
            return ResponseEntity.ok(exampleService.getExamples(lang));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/composition/examples/language/{language}/level/{level}")
    public ResponseEntity<List<CompositionExampleResponse>> getCompositionExamplesByLanguageAndLevel(
            @PathVariable String language,
            @PathVariable String level) {
        try {
            Language lang = Language.valueOf(language.toUpperCase());
            return ResponseEntity.ok(exampleService.getExamplesByLevel(lang, level));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

}
