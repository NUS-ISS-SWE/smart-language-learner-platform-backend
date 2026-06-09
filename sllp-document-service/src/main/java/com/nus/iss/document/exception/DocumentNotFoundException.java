package com.nus.iss.document.exception;

/**
 * Exception thrown when document is not found or not owned by user.
 */
public class DocumentNotFoundException extends RuntimeException {
    public DocumentNotFoundException(String documentId) {
        super("Document with ID '" + documentId + "' not found or access denied");
    }
}
