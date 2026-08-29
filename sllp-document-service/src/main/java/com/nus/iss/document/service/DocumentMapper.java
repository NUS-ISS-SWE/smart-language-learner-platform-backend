package com.nus.iss.document.service;

import org.springframework.stereotype.Component;

import com.nus.iss.document.dto.DocumentContentResponse;
import com.nus.iss.document.dto.DocumentDTO;
import com.nus.iss.document.entity.Document;

/** Keeps document response mapping independent from document workflows. */
@Component
public class DocumentMapper {

    public DocumentDTO toDto(Document document) {
        return new DocumentDTO(
                document.getId(), document.getFilename(), document.getFileType(), document.getFileSize(),
                document.getStatus(), document.getCreatedAt());
    }

    public DocumentContentResponse toContentResponse(Document document) {
        return new DocumentContentResponse(document.getId(), document.getStatus(), document.getExtractedText());
    }
}
