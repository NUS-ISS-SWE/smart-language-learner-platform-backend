package com.nus.iss.document.service;

import java.io.InputStream;
import java.time.Duration;

import org.springframework.stereotype.Service;

import com.nus.iss.document.config.S3Properties;
import com.nus.iss.document.exception.FileUploadException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

/**
 * S3 service for uploading and managing documents in AWS S3.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private static final String DOCUMENTS_PREFIX = "documents/";
    private static final long PRESIGNED_URL_DURATION_HOURS = 1;

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties properties;

    /**
     * Upload file to S3.
     *
     * @param userId user ID for partitioning
     * @param filename sanitized filename
     * @param inputStream file input stream
     * @param fileSize file size
     * @param contentType MIME type
     * @return S3 key (path) where file is stored
     */
    public String uploadFile(String userId, String filename, InputStream inputStream, long fileSize, String contentType) {
        String s3Key = DOCUMENTS_PREFIX + userId + "/" + System.nanoTime() + "_" + filename;

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(s3Key)
                .contentType(contentType)
                .contentLength(fileSize)
                .build();

            s3Client.putObject(putObjectRequest, software.amazon.awssdk.core.sync.RequestBody.fromInputStream(inputStream, fileSize));
            log.info("File uploaded to S3: s3://{}/{}", properties.bucket(), s3Key);
            return s3Key;
        } catch (S3Exception e) {
            log.error("S3 upload failed: {}", e.awsErrorDetails().errorMessage(), e);
            throw new FileUploadException("Failed to upload file to storage: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during S3 upload", e);
            throw new FileUploadException("Unexpected error during file upload", e);
        }
    }

    /**
     * Generate presigned URL for downloading file.
     *
     * @param s3Key S3 key (path)
     * @return presigned URL valid for 1 hour
     */
    public String generatePresignedUrl(String s3Key) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(properties.bucket())
                .key(s3Key)
                .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(PRESIGNED_URL_DURATION_HOURS))
                .getObjectRequest(getObjectRequest)
                .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            log.info("Presigned URL generated for: {}", s3Key);
            return presignedRequest.url().toString();
        } catch (Exception e) {
            log.error("Failed to generate presigned URL", e);
            throw new FileUploadException("Failed to generate download URL", e);
        }
    }

    /**
     * Delete file from S3.
     *
     * @param s3Key S3 key (path)
     */
    public void deleteFile(String s3Key) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(properties.bucket())
                .key(s3Key)
                .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("File deleted from S3: {}", s3Key);
        } catch (S3Exception e) {
            log.error("S3 delete failed: {}", e.awsErrorDetails().errorMessage(), e);
            throw new FileUploadException("Failed to delete file from storage", e);
        }
    }
}
