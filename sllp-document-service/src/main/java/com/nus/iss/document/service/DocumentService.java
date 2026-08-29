package com.nus.iss.document.service;

import java.io.IOException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.nus.iss.document.dto.DocumentContentResponse;
import com.nus.iss.document.dto.DocumentDTO;
import com.nus.iss.document.dto.DocumentUploadResponse;
import com.nus.iss.document.dto.DocumentsPageResponse;
import com.nus.iss.document.entity.Document;
import com.nus.iss.document.exception.DocumentNotFoundException;
import com.nus.iss.document.repository.DocumentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Document service for managing user document uploads and text extraction.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final S3Service s3Service;
    private final FileValidationService fileValidationService;
    private final TextExtractionService textExtractionService;
    private final DocumentAccessService documentAccessService;
    private final DocumentMapper documentMapper;

    /**
     * Upload document for user, then synchronously extract text.
     *
     * @param userIdStr authenticated user ID string from JWT userId claim
     * @param file      multipart file to upload
     * @return upload response with document details and download URL
     */
    @Transactional
    public DocumentUploadResponse uploadDocument(String userIdStr, MultipartFile file) {
        log.info("Uploading document for user: {}", userIdStr);
        Long userId = documentAccessService.userId(userIdStr);

        fileValidationService.validateFile(file);

        try {
            String originalFilename = file.getOriginalFilename();
            String sanitizedFilename = fileValidationService.sanitizeFilename(originalFilename);
            String fileExtension = fileValidationService.getFileExtension(originalFilename);

            // Read bytes once - used for both S3 upload and text extraction
            byte[] fileBytes = file.getBytes();

            // Upload to S3 (use userIdStr as the S3 key prefix)
            String s3Key = s3Service.uploadFile(
                userIdStr,
                sanitizedFilename,
                new java.io.ByteArrayInputStream(fileBytes),
                fileBytes.length,
                file.getContentType()
            );

            // Persist metadata with PROCESSING status
            Document document = Document.builder()
                .userId(userId)
                .filename(originalFilename)
                .s3Key(s3Key)
                .fileType(fileExtension)
                .fileSize(file.getSize())
                .status("PROCESSING")
                .build();

            document = documentRepository.save(document);
            log.info("Document saved: id={}, userId={}", document.getId(), userId);

            // Extract text synchronously
            String extractedText = textExtractionService.extract(
                new java.io.ByteArrayInputStream(fileBytes), originalFilename);

            document.setExtractedText(extractedText);
            document.setStatus(extractedText != null ? "READY" : "FAILED");
            document = documentRepository.save(document);

            String downloadUrl = s3Service.generatePresignedUrl(s3Key);
            return new DocumentUploadResponse(documentMapper.toDto(document), downloadUrl);
        } catch (IOException e) {
            log.error("Error reading file", e);
            throw new IllegalStateException("Error processing file", e);
        }
    }

    @Transactional(readOnly = true)
    public DocumentContentResponse getDocumentContent(String userIdStr, Long documentId) {
        Long userId = documentAccessService.userId(userIdStr);
        log.info("Retrieving content for document: id={}, userId={}", documentId, userId);
        return documentMapper.toContentResponse(documentAccessService.requireOwned(documentId, userId));
    }

    @Transactional(readOnly = true)
    public DocumentUploadResponse getDocument(String userIdStr, Long documentId) {
        Long userId = documentAccessService.userId(userIdStr);
        log.info("Retrieving document: id={}, userId={}", documentId, userId);
        return toUploadResponse(documentAccessService.requireOwned(documentId, userId));
    }

    @Transactional(readOnly = true)
    public DocumentUploadResponse getDocumentForAdmin(Long documentId) {
        log.info("Admin retrieving document: id={}", documentId);
        return toUploadResponse(documentAccessService.require(documentId));
    }

    @Transactional(readOnly = true)
    public DocumentContentResponse getDocumentContentForAdmin(Long documentId) {
        log.info("Admin retrieving document content: id={}", documentId);
        return documentMapper.toContentResponse(documentAccessService.require(documentId));
    }

    private DocumentUploadResponse toUploadResponse(Document document) {
        return new DocumentUploadResponse(documentMapper.toDto(document), s3Service.generatePresignedUrl(document.getS3Key()));
    }

    /**
     * List user documents (paginated).
     *
     * @param userIdStr authenticated user ID string from JWT userId claim
     * @param page      page number (0-based)
     * @param size      page size
     * @return paginated list of documents
     */
    @Transactional(readOnly = true)
    public DocumentsPageResponse listUserDocuments(String userIdStr, int page, int size) {
        Long userId = documentAccessService.userId(userIdStr);
        log.info("Listing documents for user: {}, page={}, size={}", userId, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<Document> documentsPage = documentRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        var documents = documentsPage.getContent()
            .stream()
            .map(documentMapper::toDto)
            .toList();

        return new DocumentsPageResponse(documents, documentsPage.getTotalElements(), page, size, documentsPage.hasNext());
    }

    /**
     * Delete document (verify ownership).
     *
     * @param userIdStr  authenticated user ID string from JWT userId claim
     * @param documentId document ID
     */
    @Transactional
    public void deleteDocument(String userIdStr, Long documentId) {
        Long userId = documentAccessService.userId(userIdStr);
        log.info("Deleting document: id={}, userId={}", documentId, userId);

        Document document = documentAccessService.requireOwned(documentId, userId);

        s3Service.deleteFile(document.getS3Key());
        documentRepository.deleteByIdAndUserId(documentId, userId);
        log.info("Document deleted: id={}", documentId);
    }
}
