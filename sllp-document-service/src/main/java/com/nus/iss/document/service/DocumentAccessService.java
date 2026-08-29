package com.nus.iss.document.service;

import org.springframework.stereotype.Service;

import com.nus.iss.document.entity.Document;
import com.nus.iss.document.exception.DocumentNotFoundException;
import com.nus.iss.document.repository.DocumentRepository;

/** Owns document lookup and ownership checks shared by query and command paths. */
@Service
public class DocumentAccessService {
    private final DocumentRepository documentRepository;

    public DocumentAccessService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public Long userId(String userId) {
        try {
            long parsed = Long.parseLong(userId);
            if (parsed < 1) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Valid user ID is required");
        }
    }

    public Document requireOwned(Long documentId, Long userId) {
        return documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new DocumentNotFoundException(String.valueOf(documentId)));
    }

    public Document require(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new DocumentNotFoundException(String.valueOf(documentId)));
    }
}
