package com.nus.iss.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.nus.iss.dto.ModerationQueueResponse;
import com.nus.iss.service.ModerationQueueService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/moderation")
@RequiredArgsConstructor
public class ModerationQueueController {
    private final ModerationQueueService moderationQueueService;

    @GetMapping
    public ResponseEntity<ModerationQueueResponse> list(
            @RequestHeader("X-Sllp-Role") String role,
            @RequestParam(required = false) String moderationStatus,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String compositionSource,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (!"ADMIN".equals(role)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        return ResponseEntity.ok(moderationQueueService.list(moderationStatus, userId, compositionSource, language,
                from, to, page, size));
    }
}
