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
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
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

    @Mock
    private TextExtractionService textExtractionService;

    private DocumentService documentService;

    private Document testDocument;
    private String testUserId = "123";
    private Long testDocumentId = 456L;



    @BeforeEach
    void setUp() {
        documentService = new DocumentService(
            documentRepository,
            s3Service,
            fileValidationService,
            textExtractionService,
            new DocumentAccessService(documentRepository),
            new DocumentMapper()
        );
        testDocument = Document.builder()
            .id(testDocumentId)
            .userId(Long.parseLong(testUserId))
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
        verify(documentRepository, times(2)).save(any(Document.class));
    }

    @Test
    void testUploadDocument_RecordsFailedProcessingWhenExtractionThrows() throws IOException {
        MultipartFile mockFile = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "PDF content".getBytes());
        doNothing().when(fileValidationService).validateFile(mockFile);
        when(fileValidationService.sanitizeFilename(anyString())).thenReturn("test.pdf");
        when(fileValidationService.getFileExtension(anyString())).thenReturn("pdf");
        when(s3Service.uploadFile(anyString(), anyString(), any(), anyLong(), anyString()))
                .thenReturn("documents/user-123/1234567890_test.pdf");
        when(documentRepository.save(any(Document.class))).thenReturn(testDocument);
        when(textExtractionService.extract(any(), anyString())).thenThrow(new RuntimeException("Parser unavailable"));

        DocumentUploadResponse response = documentService.uploadDocument(testUserId, mockFile);

        assertEquals("FAILED", response.document().status());
        assertNotNull(testDocument.getProcessedAt());
        verify(documentRepository, times(2)).save(any(Document.class));
    }

    @Test
    void testGetDocument_Success() {
        // Arrange
        when(documentRepository.findByIdAndUserId(testDocumentId, Long.parseLong(testUserId)))
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
        when(documentRepository.findByIdAndUserId(testDocumentId, Long.parseLong(testUserId)))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(DocumentNotFoundException.class,
            () -> documentService.getDocument(testUserId, testDocumentId));
    }

    @Test
    void testDeleteDocument_Success() {
        // Arrange
        when(documentRepository.findByIdAndUserId(testDocumentId, Long.parseLong(testUserId)))
            .thenReturn(Optional.of(testDocument));
        doNothing().when(s3Service).deleteFile(testDocument.getS3Key());
        when(documentRepository.deleteByIdAndUserId(testDocumentId, Long.parseLong(testUserId)))
            .thenReturn(1L);

        // Act
        assertDoesNotThrow(() -> documentService.deleteDocument(testUserId, testDocumentId));

        // Assert
        verify(s3Service).deleteFile(testDocument.getS3Key());
        verify(documentRepository).deleteByIdAndUserId(testDocumentId, Long.parseLong(testUserId));
    }

    @Test
    void testDeleteDocument_NotFound() {
        // Arrange
        when(documentRepository.findByIdAndUserId(testDocumentId, Long.parseLong(testUserId)))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(DocumentNotFoundException.class,
            () -> documentService.deleteDocument(testUserId, testDocumentId));
    }
}
