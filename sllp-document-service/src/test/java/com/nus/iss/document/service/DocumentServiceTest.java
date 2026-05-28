package com.nus.iss.document.service;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.nus.iss.document.dto.DocumentUploadResponse;
import com.nus.iss.document.entity.Document;
import com.nus.iss.document.exception.DocumentNotFoundException;
import com.nus.iss.document.repository.DocumentRepository;

/**
 * Unit tests for DocumentService.
 */
@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private S3Service s3Service;

    @Mock
    private FileValidationService fileValidationService;

    @InjectMocks
    private DocumentService documentService;

    private Document testDocument;
    private String testUserId = "user-123";
    private String testDocumentId = "doc-456";

    @BeforeEach
    void setUp() {
        testDocument = Document.builder()
            .id(testDocumentId)
            .userId(testUserId)
            .filename("test.pdf")
            .s3Key("documents/user-123/1234567890_test.pdf")
            .fileType("pdf")
            .fileSize(1024000L)
            .status("UPLOADED")
            .build();
    }

    @Test
    void testUploadDocument_Success() throws IOException {
        // Arrange
        MultipartFile mockFile = new MockMultipartFile(
            "file",
            "test.pdf",
            "application/pdf",
            "PDF content".getBytes()
        );

        doNothing().when(fileValidationService).validateFile(mockFile);
        when(fileValidationService.sanitizeFilename(anyString())).thenReturn("test.pdf");
        when(fileValidationService.getFileExtension(anyString())).thenReturn("pdf");
        when(s3Service.uploadFile(anyString(), anyString(), any(), anyLong(), anyString()))
            .thenReturn("documents/user-123/1234567890_test.pdf");
        when(documentRepository.save(any(Document.class))).thenReturn(testDocument);
        when(s3Service.generatePresignedUrl(anyString())).thenReturn("https://s3.amazonaws.com/...");

        // Act
        DocumentUploadResponse response = documentService.uploadDocument(testUserId, mockFile);

        // Assert
        assertNotNull(response);
        assertEquals("test.pdf", response.document().filename());
        assertEquals("pdf", response.document().fileType());
        assertNotNull(response.downloadUrl());
        verify(documentRepository).save(any(Document.class));
    }

    @Test
    void testGetDocument_Success() {
        // Arrange
        when(documentRepository.findByIdAndUserId(testDocumentId, testUserId))
            .thenReturn(Optional.of(testDocument));
        when(s3Service.generatePresignedUrl(testDocument.getS3Key()))
            .thenReturn("https://s3.amazonaws.com/...");

        // Act
        DocumentUploadResponse response = documentService.getDocument(testUserId, testDocumentId);

        // Assert
        assertNotNull(response);
        assertEquals("test.pdf", response.document().filename());
        assertNotNull(response.downloadUrl());
    }

    @Test
    void testGetDocument_NotFound() {
        // Arrange
        when(documentRepository.findByIdAndUserId(testDocumentId, testUserId))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(DocumentNotFoundException.class,
            () -> documentService.getDocument(testUserId, testDocumentId));
    }

    @Test
    void testDeleteDocument_Success() {
        // Arrange
        when(documentRepository.findByIdAndUserId(testDocumentId, testUserId))
            .thenReturn(Optional.of(testDocument));
        doNothing().when(s3Service).deleteFile(testDocument.getS3Key());
        when(documentRepository.deleteByIdAndUserId(testDocumentId, testUserId))
            .thenReturn(1L);

        // Act
        assertDoesNotThrow(() -> documentService.deleteDocument(testUserId, testDocumentId));

        // Assert
        verify(s3Service).deleteFile(testDocument.getS3Key());
        verify(documentRepository).deleteByIdAndUserId(testDocumentId, testUserId);
    }

    @Test
    void testDeleteDocument_NotFound() {
        // Arrange
        when(documentRepository.findByIdAndUserId(testDocumentId, testUserId))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(DocumentNotFoundException.class,
            () -> documentService.deleteDocument(testUserId, testDocumentId));
    }
}
