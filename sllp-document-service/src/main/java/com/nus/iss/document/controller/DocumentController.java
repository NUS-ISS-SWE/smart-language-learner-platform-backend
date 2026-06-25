package com.nus.iss.document.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.nus.iss.document.dto.DocumentContentResponse;
import com.nus.iss.document.dto.DocumentUploadResponse;
import com.nus.iss.document.dto.DocumentsPageResponse;
import com.nus.iss.document.security.JwtAuthenticationToken;
import com.nus.iss.document.service.DocumentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST controller for document management endpoints.
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Documents", description = "Document upload and management endpoints")
public class DocumentController {

    private final DocumentService documentService;

    /**
     * Upload document endpoint.
     *
     * @param authentication current user authentication
     * @param file multipart file to upload
     * @return upload response with document details
     */
    @PostMapping("/upload")
    @Operation(summary = "Upload document", description = "Upload a document file (PDF, DOCX, PPTX, TXT)")
    public ResponseEntity<DocumentUploadResponse> uploadDocument(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) {
        
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("Upload endpoint called for user: {}", jwtAuth.getUserId());
        DocumentUploadResponse response = documentService.uploadDocument(jwtAuth.getUserId(), file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get document by ID endpoint.
     *
     * @param authentication current user authentication
     * @param id document ID
     * @return document details with download URL
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get document", description = "Retrieve document details and download URL")
    public ResponseEntity<DocumentUploadResponse> getDocument(
            Authentication authentication,
            @PathVariable Long id) {
        
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("Get document endpoint called for user: {}", jwtAuth.getUserId());
        DocumentUploadResponse response = documentService.getDocument(jwtAuth.getUserId(), id);
        return ResponseEntity.ok(response);
    }

    /**
     * List user's documents endpoint (paginated).
     *
     * @param authentication current user authentication
     * @param page page number (0-indexed, default 0)
     * @param size page size (default 20)
     * @return paginated documents
     */
    @GetMapping
    @Operation(summary = "List documents", description = "List all documents for authenticated user (paginated)")
    public ResponseEntity<DocumentsPageResponse> listDocuments(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("List documents endpoint called for user: {}", jwtAuth.getUserId());
        DocumentsPageResponse response = documentService.listUserDocuments(jwtAuth.getUserId(), page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Get extracted text content for a document.
     *
     * @param authentication current user authentication
     * @param id document ID
     * @return extracted text content and processing status
     */
    @GetMapping("/{id}/content")
    @Operation(summary = "Get document content", description = "Retrieve extracted plain-text content of a document")
    public ResponseEntity<DocumentContentResponse> getDocumentContent(
            Authentication authentication,
            @PathVariable Long id) {

        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("Get document content called for user: {}, document: {}", jwtAuth.getUserId(), id);
        DocumentContentResponse response = documentService.getDocumentContent(jwtAuth.getUserId(), id);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete document endpoint.
     *
     * @param authentication current user authentication
     * @param id document ID
     * @return no content response
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete document", description = "Delete a document owned by authenticated user")
    public ResponseEntity<Void> deleteDocument(
            Authentication authentication,
            @PathVariable Long id) {
        
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("Delete document endpoint called for user: {}", jwtAuth.getUserId());
        documentService.deleteDocument(jwtAuth.getUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
