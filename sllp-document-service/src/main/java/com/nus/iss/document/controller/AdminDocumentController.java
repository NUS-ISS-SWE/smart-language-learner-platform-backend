package com.nus.iss.document.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.nus.iss.document.dto.DocumentsPageResponse;
import com.nus.iss.document.service.DocumentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/documents/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Documents", description = "Admin endpoints for managing user documents")
public class AdminDocumentController {

    private final DocumentService documentService;

    @GetMapping
    @Operation(summary = "List documents for any user (admin only)")
    public ResponseEntity<DocumentsPageResponse> getUserDocuments(
            @RequestHeader("X-Sllp-Role") String role,
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        assertAdmin(role);
        log.info("Admin retrieving documents for userId={}", userId);
        DocumentsPageResponse response = documentService.listUserDocuments(String.valueOf(userId), page, size);
        return ResponseEntity.ok(response);
    }

    private static void assertAdmin(String role) {
        if (!"ADMIN".equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
    }
}
