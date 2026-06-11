package com.nus.iss.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response containing extracted text content from a document.
 *
 * @param id document ID
 * @param status document processing status (PROCESSING, READY, FAILED)
 * @param extractedText extracted plain text content, or null if not yet available
 */
public record DocumentContentResponse(
    @Schema(description = "Document ID")
    String id,

    @Schema(description = "Document status", example = "READY")
    String status,

    @Schema(description = "Extracted plain text content", nullable = true)
    String extractedText
) { }
