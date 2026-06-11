package com.nus.iss.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Document data transfer object.
 *
 * @param id document ID
 * @param filename original filename
 * @param fileType file type (pdf, docx, pptx, txt)
 * @param fileSize file size in bytes
 * @param status document processing status (UPLOADED, PROCESSING, READY, FAILED)
 * @param createdAt creation timestamp
 */
public record DocumentDTO(
    @Schema(description = "Document ID", example = "550e8400-e29b-41d4-a716-446655440000")
    String id,

    @Schema(description = "Original filename", example = "essay.pdf")
    String filename,

    @Schema(description = "File type", example = "pdf")
    String fileType,

    @Schema(description = "File size in bytes", example = "1024000")
    long fileSize,

    @Schema(description = "Document processing status", example = "READY")
    String status,

    @Schema(description = "Creation timestamp")
    Instant createdAt
) { }
