package com.nus.iss.document.config;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * AWS S3 configuration for document storage.
 * Supports both real AWS S3 and local S3-compatible services (e.g. MinIO).
 * Set aws.s3.endpoint to the internal service URL (e.g. http://minio:9000) for MinIO.
 * Set aws.s3.public-endpoint to the browser-accessible URL (e.g. http://localhost:9000)
 * so presigned download URLs work from the frontend.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class S3Config {

    @Value("${aws.s3.region}")
    private String awsRegion;

    @Value("${aws.s3.access-key}")
    private String accessKey;

    @Value("${aws.s3.secret-key}")
    private String secretKey;

    /** Optional: override the S3 endpoint for local S3-compatible services (e.g. MinIO). */
    @Value("${aws.s3.endpoint:}")
    private String s3Endpoint;

    /** Optional: override endpoint used when generating presigned URLs (browser-accessible). */
    @Value("${aws.s3.public-endpoint:}")
    private String s3PublicEndpoint;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    /**
     * Create S3 client bean.
     *
     * @return S3Client
     */
    @Bean
    public S3Client s3Client() {
        var builder = S3Client.builder().region(Region.of(awsRegion));
        if (hasValidCredentials()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)));
        }
        if (!s3Endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(s3Endpoint));
            // Path-style access is required for MinIO and other S3-compatible services
            builder.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
            log.info("S3 client using custom endpoint: {}", s3Endpoint);
        }
        return builder.build();
    }

    /**
     * Create S3 presigner bean for generating presigned URLs.
     * Uses the public endpoint (if set) so generated URLs are accessible from browsers.
     *
     * @return S3Presigner
     */
    @Bean
    public S3Presigner s3Presigner() {
        // Use the public endpoint for presigned URLs so browsers can resolve the host.
        // Falls back to the internal endpoint if no public endpoint is configured.
        String presignerEndpoint = !s3PublicEndpoint.isEmpty() ? s3PublicEndpoint
                                 : !s3Endpoint.isEmpty()       ? s3Endpoint
                                 : null;
        var builder = S3Presigner.builder().region(Region.of(awsRegion));
        if (hasValidCredentials()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)));
        }
        if (presignerEndpoint != null) {
            builder.endpointOverride(URI.create(presignerEndpoint));
            builder.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
            log.info("S3 presigner using public endpoint: {}", presignerEndpoint);
        }
        return builder.build();
    }

    /**
     * Ensure the S3 bucket exists at startup. Only runs when a custom endpoint is
     * configured (i.e. MinIO / local S3) — real AWS buckets are managed externally.
     */
    @Bean
    public ApplicationRunner ensureBucketExists(S3Client s3Client) {
        return args -> {
            if (s3Endpoint.isEmpty()) {
                return; // Real AWS — bucket management is out of scope for the app
            }
            try {
                s3Client.headBucket(b -> b.bucket(bucketName));
                log.info("S3 bucket '{}' already exists", bucketName);
            } catch (Exception e) {
                log.info("S3 bucket '{}' not found, creating it", bucketName);
                s3Client.createBucket(b -> b.bucket(bucketName));
                log.info("S3 bucket '{}' created", bucketName);
            }
        };
    }

    /**
     * Check if AWS credentials are properly configured.
     *
     * @return true if both access key and secret key are provided and non-empty
     */
    private boolean hasValidCredentials() {
        return accessKey != null && !accessKey.isEmpty() && secretKey != null && !secretKey.isEmpty();
    }
}
