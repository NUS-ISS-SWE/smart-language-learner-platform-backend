package com.nus.iss.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Error response DTO.
 *
 * @param code error code
 * @param message error message
 */
public record ErrorResponse(
    @Schema(description = "Error code", example = "DOCUMENT_NOT_FOUND")
    String code,

    @Schema(description = "Error message", example = "Document not found or access denied")
    String message
) { }
