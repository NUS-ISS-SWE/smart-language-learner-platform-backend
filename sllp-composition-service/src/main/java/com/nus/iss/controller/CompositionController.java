package com.nus.iss.controller;

import com.nus.iss.dto.CompositionDTO;
import com.nus.iss.service.CompositionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1")
public class CompositionController {

    private final CompositionService compositionService;

    @Autowired
    public CompositionController(CompositionService compositionService) {
        this.compositionService = compositionService;
    }

    @PostMapping("/composition")
    public ResponseEntity<CompositionDTO> createNewComposition(@RequestHeader("X-Sllp-Username") String username,
                                                               @RequestHeader("X-Sllp-Role") String role,
                                                               @RequestBody CompositionDTO compositionDTO) {

        log.info("Creating new composition for user: {} with role: {}, title: {}, contentPreview: {}",
                username, role, compositionDTO.getTitle(), getContentPreview(compositionDTO.getContent()));
        CompositionDTO createdComposition = compositionService.createNewComposition(username, compositionDTO);
        return ResponseEntity.ok(createdComposition);
    }

    @GetMapping("/composition")
    public ResponseEntity<List<CompositionDTO>> getAllCompositionByUsername(@RequestHeader("X-Sllp-Username") String username,
                                                                            @RequestHeader("X-Sllp-Role") String role,
                                                                            @RequestParam(defaultValue = "0") int page,
                                                                            @RequestParam(defaultValue = "10") int size) {
        log.info("Getting compositions for user: {} with role: {}, page: {}, size: {}", username, role, page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<CompositionDTO> compositions = compositionService.getAllCompositionByUsername(username, pageable);
        return ResponseEntity.ok(compositions);
    }

    @GetMapping("/composition/{id}")
    public ResponseEntity<CompositionDTO> getSingleCompositionById(@RequestHeader("X-Sllp-Username") String username,
                                                                   @RequestHeader("X-Sllp-Role") String role,
                                                                   @PathVariable Long id) {
        log.info("Getting composition with ID: {} for user: {} with role: {}", id, username, role);
        CompositionDTO composition = compositionService.getSingleCompositionById(username, id);
        return ResponseEntity.ok(composition);
    }

    @PutMapping("/composition/{id}")
    public ResponseEntity<CompositionDTO> updateComposition(@RequestHeader("X-Sllp-Username") String username,
                                                            @RequestHeader("X-Sllp-Role") String role,
                                                            @PathVariable Long id,
                                                            @RequestBody CompositionDTO compositionDTO) {
        log.info("Updating composition with ID: {} for user: {} with role: {}, title: {}, contentPreview: {}",
                id, username, role, compositionDTO.getTitle(), getContentPreview(compositionDTO.getContent()));
        CompositionDTO updatedComposition = compositionService.updateComposition(username, id, compositionDTO);
        return ResponseEntity.ok(updatedComposition);
    }

    @DeleteMapping("/composition/{id}")
    public ResponseEntity<CompositionDTO> deleteComposition(@RequestHeader("X-Sllp-Username") String username,
                                               @RequestHeader("X-Sllp-Role") String role,
                                               @PathVariable Long id) {
        log.info("Deleting composition with ID: {} for user: {} with role: {}", id, username, role);
        compositionService.deleteComposition(username, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/composition/{id}/grading")
    public ResponseEntity<?> requestToGradeComposition(@RequestHeader("X-Sllp-Username") String username,
                                                       @RequestHeader("X-Sllp-Role") String role,
                                                       @PathVariable Long id) {
        log.info("Requesting grading for composition with ID: {} for user: {} with role: {}", id, username, role);
        CompositionDTO composition = compositionService.requestToGradeComposition(username, id);
        return ResponseEntity.ok(composition);
    }


    @PostMapping("/composition/generate")
    public ResponseEntity<CompositionDTO> generateComposition(@RequestHeader("X-Sllp-Username") String username,
                                                              @RequestHeader("X-Sllp-Role") String role,
                                                              @RequestBody CompositionDTO compositionDTO) {
        log.info("Generating composition for user: {} with role: {}, title: {}",
                username, role, compositionDTO.getTitle());
        CompositionDTO composition = compositionService.generateComposition(username, compositionDTO);
        return ResponseEntity.ok(composition);
    }


    private String getContentPreview(String content) {
        if (content == null) {
            return "";
        }

        return content.substring(0, Math.min(content.length(), 20));
    }
}
