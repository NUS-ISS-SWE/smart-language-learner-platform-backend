package com.nus.iss.document.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import com.nus.iss.document.exception.FileUploadException;

/**
 * Unit tests for FileValidationService.
 */
class FileValidationServiceTest {

    private FileValidationService fileValidationService;

    @BeforeEach
    void setUp() {
        fileValidationService = new FileValidationService();
        ReflectionTestUtils.setField(fileValidationService, "maxFileSize", 52428800L); // 50MB
    }

    @Test
    void testValidateFile_ValidPDF() {
        // Arrange
        MultipartFile file = new MockMultipartFile(
            "file", 
            "document.pdf", 
            "application/pdf", 
            "PDF content".getBytes()
        );

        // Act & Assert
        assertDoesNotThrow(() -> fileValidationService.validateFile(file));
    }

    @Test
    void testValidateFile_ValidDOCX() {
        // Arrange
        MultipartFile file = new MockMultipartFile(
            "file",
            "document.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "DOCX content".getBytes()
        );

        // Act & Assert
        assertDoesNotThrow(() -> fileValidationService.validateFile(file));
    }

    @Test
    void testValidateFile_EmptyFile() {
        // Arrange
        MultipartFile file = new MockMultipartFile("file", "", "application/pdf", new byte[0]);

        // Act & Assert
        assertThrows(FileUploadException.class, () -> fileValidationService.validateFile(file));
    }

    @Test
    void testValidateFile_FileTooLarge() {
        // Arrange
        byte[] largeContent = new byte[52428801]; // Just over 50MB limit
        MultipartFile file = new MockMultipartFile(
            "file",
            "large.pdf",
            "application/pdf",
            largeContent
        );

        // Act & Assert
        assertThrows(FileUploadException.class, () -> fileValidationService.validateFile(file));
    }

    @Test
    void testValidateFile_InvalidContentType() {
        // Arrange
        MultipartFile file = new MockMultipartFile(
            "file",
            "script.exe",
            "application/x-msdownload",
            "malicious".getBytes()
        );

        // Act & Assert
        assertThrows(FileUploadException.class, () -> fileValidationService.validateFile(file));
    }

    @Test
    void testValidateFile_PathTraversalAttempt() {
        // Arrange
        MultipartFile file = new MockMultipartFile(
            "file",
            "../../../etc/passwd",
            "text/plain",
            "content".getBytes()
        );

        // Act & Assert
        assertThrows(FileUploadException.class, () -> fileValidationService.validateFile(file));
    }

    @Test
    void testSanitizeFilename() {
        // Arrange & Act
        String sanitized = fileValidationService.sanitizeFilename("my doc-2024 (final).pdf");

        // Assert
        assertTrue(sanitized.contains("my"));
        assertTrue(sanitized.endsWith("pdf"));
        assertFalse(sanitized.contains("("));
        assertFalse(sanitized.contains(")"));
    }

    @Test
    void testGetFileExtension() {
        // Act & Assert
        assertEquals("pdf", fileValidationService.getFileExtension("document.pdf"));
        assertEquals("docx", fileValidationService.getFileExtension("document.docx"));
        assertEquals("txt", fileValidationService.getFileExtension("document.txt"));
    }
}
