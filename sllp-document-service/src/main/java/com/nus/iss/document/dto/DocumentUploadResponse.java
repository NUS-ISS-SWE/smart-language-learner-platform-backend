package com.nus.iss.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Document upload response.
 *
 * @param document uploaded document DTO
 * @param downloadUrl presigned download URL (valid for 1 hour)
 */
public record DocumentUploadResponse(
    @Schema(description = "Document details")
    DocumentDTO document,

    @Schema(description = "Presigned download URL", example = "https://s3.amazonaws.com/...")
    String downloadUrl
) { }
