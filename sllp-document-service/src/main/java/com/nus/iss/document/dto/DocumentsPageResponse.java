package com.nus.iss.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Paginated documents response.
 *
 * @param documents list of documents
 * @param total total number of documents
 * @param page current page
 * @param pageSize page size
 * @param hasMore whether there are more pages
 */
public record DocumentsPageResponse(
    @Schema(description = "List of documents")
    List<DocumentDTO> documents,

    @Schema(description = "Total number of documents", example = "42")
    long total,

    @Schema(description = "Current page", example = "0")
    int page,

    @Schema(description = "Page size", example = "20")
    int pageSize,

    @Schema(description = "Whether there are more pages")
    boolean hasMore
) { }
