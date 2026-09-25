package com.nus.iss.document.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import com.nus.iss.document.config.S3Config;
import com.nus.iss.document.config.S3Properties;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Verifies the document-storage path against the LocalStack service started by
 * docker-compose.override.yaml. Enable explicitly after starting LocalStack so normal
 * unit-test runs do not require Docker.
 */
@EnabledIfEnvironmentVariable(named = "RUN_S3_INTEGRATION_TESTS", matches = "true")
class S3ServiceLocalStackIntegrationTest {

    private static final String BUCKET = "sllp-document-service-test-" + UUID.randomUUID();
    private static final String LOCALSTACK_ENDPOINT = System.getenv()
        .getOrDefault("LOCALSTACK_S3_ENDPOINT", "http://localhost:4566");

    private S3Client s3Client;
    private S3Presigner s3Presigner;
    private S3Service s3Service;

    @BeforeEach
    void setUp() throws Exception {
        S3Properties properties = new S3Properties(
            "us-east-1", "test", "test", "", LOCALSTACK_ENDPOINT, LOCALSTACK_ENDPOINT, BUCKET
        );
        S3Config config = new S3Config(properties);

        s3Client = config.s3Client();
        s3Presigner = config.s3Presigner();
        config.ensureBucketExists(s3Client).run(null);

        s3Service = new S3Service(s3Client, s3Presigner, properties);
    }

    @AfterEach
    void tearDown() {
        if (s3Presigner != null) {
            s3Presigner.close();
        }
        if (s3Client != null) {
            s3Client.close();
        }
    }

    @Test
    void uploadsDownloadsWithPresignedUrlAndDeletesObject() throws Exception {
        byte[] content = "localstack document upload".getBytes(StandardCharsets.UTF_8);
        String s3Key = s3Service.uploadFile(
            "user-123",
            "note.txt",
            new ByteArrayInputStream(content),
            content.length,
            "text/plain"
        );

        assertTrue(s3Key.startsWith("documents/user-123/"));
        assertEquals("localstack document upload", s3Client.getObjectAsBytes(b -> b
            .bucket(BUCKET)
            .key(s3Key)).asUtf8String());

        HttpResponse<String> response = HttpClient.newHttpClient().send(
            HttpRequest.newBuilder(URI.create(s3Service.generatePresignedUrl(s3Key))).GET().build(),
            HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, response.statusCode());
        assertEquals("localstack document upload", response.body());

        s3Service.deleteFile(s3Key);
        assertThrows(NoSuchKeyException.class, () -> s3Client.getObjectAsBytes(b -> b
            .bucket(BUCKET)
            .key(s3Key)));
    }
}
