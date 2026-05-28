package com.nus.iss.document.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.nus.iss.document.exception.FileUploadException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * File validation service for security checks on uploaded files.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileValidationService {

    @Value("${application.file.max-size}")
    private long maxFileSize;

    private static final Set<String> ALLOWED_MIME_TYPES = new HashSet<>(Arrays.asList(
        "application/pdf",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "text/plain"
    ));

    private static final Set<String> FORBIDDEN_FILENAME_CHARS = new HashSet<>(Arrays.asList(
        "..", "/", "\\", "\0", "\n", "\r"
    ));

    /**
     * Validate uploaded file.
     *
     * @param file multipart file to validate
     * @throws FileUploadException if validation fails
     */
    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileUploadException("File is empty or not provided");
        }

        // Validate file size
        if (file.getSize() > maxFileSize) {
            throw new FileUploadException("File size exceeds maximum allowed size of " + (maxFileSize / 1024 / 1024) + "MB");
        }

        // Sanitize and validate filename
        String filename = file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) {
            throw new FileUploadException("Filename is invalid");
        }

        // Prevent path traversal
        if (FORBIDDEN_FILENAME_CHARS.stream().anyMatch(filename::contains)) {
            throw new FileUploadException("Filename contains forbidden characters");
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new FileUploadException("File type not allowed. Supported types: PDF, DOCX, PPTX, TXT");
        }

        log.info("File validation passed: {}", filename);
    }

    /**
     * Sanitize filename for S3 storage.
     *
     * @param filename original filename
     * @return sanitized filename
     */
    public String sanitizeFilename(String filename) {
        return filename
            .replaceAll("[^a-zA-Z0-9._-]", "_")
            .replaceAll("_{2,}", "_");
    }

    /**
     * Get file extension from filename.
     *
     * @param filename filename
     * @return file extension (e.g., "pdf", "docx")
     */
    public String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            return filename.substring(lastDot + 1).toLowerCase();
        }
        return "";
    }
}
