package com.nus.iss.controller;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.nus.iss.dto.CompositionDTO;
import com.nus.iss.service.CompositionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminCompositionController {

    private final CompositionService compositionService;

    @GetMapping("/compositions")
    public ResponseEntity<List<CompositionDTO>> getUserCompositions(
            @RequestHeader("X-Sllp-Role") String role,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        assertAdmin(role);
        log.info("Admin retrieving compositions for userId={}", userId);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<CompositionDTO> compositions = compositionService.getAllCompositionByUserId(userId, pageable);
        return ResponseEntity.ok(compositions);
    }

    private static void assertAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }
}
