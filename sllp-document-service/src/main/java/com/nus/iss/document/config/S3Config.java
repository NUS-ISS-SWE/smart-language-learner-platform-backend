package com.nus.iss.document.config;

import java.net.URI;

import jakarta.annotation.PostConstruct;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * AWS S3 configuration for document storage.
 * Uses native AWS S3 by default. A custom endpoint is available only for an
 * explicitly approved S3-compatible service. Set aws.s3.public-endpoint to a
 * browser-accessible custom endpoint when presigned download URLs require it.
 */
@Configuration
@EnableConfigurationProperties(S3Properties.class)
@Slf4j
public class S3Config {

    private final S3Properties properties;

    public S3Config(S3Properties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void validateRequiredConfiguration() {
        if (isBlank(properties.bucket())) {
            throw new IllegalStateException("AWS_S3_BUCKET must be configured for document storage");
        }
        if (isBlank(properties.region())) {
            throw new IllegalStateException("AWS_REGION must be configured for document storage");
        }
        if (hasAnyStaticCredential() && !hasValidCredentials()) {
            throw new IllegalStateException(
                "AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY must be configured together"
            );
        }
        if (!isBlank(properties.sessionToken()) && !hasValidCredentials()) {
            throw new IllegalStateException(
                "AWS_SESSION_TOKEN requires AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY"
            );
        }
    }

    /**
     * Create S3 client bean.
     *
     * @return S3Client
     */
    @Bean
    public S3Client s3Client() {
        var builder = S3Client.builder().region(Region.of(properties.region()));
        if (hasValidCredentials()) {
            builder.credentialsProvider(staticCredentialsProvider());
        }
        if (!isBlank(properties.endpoint())) {
            builder.endpointOverride(URI.create(properties.endpoint()));
            // Path-style access is required by many S3-compatible services.
            builder.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
            log.info("S3 client using custom endpoint: {}", properties.endpoint());
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
        String presignerEndpoint = !isBlank(properties.publicEndpoint()) ? properties.publicEndpoint()
                                 : !isBlank(properties.endpoint())       ? properties.endpoint()
                                 : null;
        var builder = S3Presigner.builder().region(Region.of(properties.region()));
        if (hasValidCredentials()) {
            builder.credentialsProvider(staticCredentialsProvider());
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
     * configured; real AWS buckets are managed externally.
     */
    @Bean
    public ApplicationRunner ensureBucketExists(S3Client s3Client) {
        return args -> {
            if (isBlank(properties.endpoint())) {
                return; // Real AWS bucket management is out of scope for the app.
            }
            try {
                s3Client.headBucket(b -> b.bucket(properties.bucket()));
                log.info("S3 bucket '{}' already exists", properties.bucket());
            } catch (S3Exception e) {
                if (e.statusCode() != 404) {
                    throw e;
                }
                log.info("S3 bucket '{}' not found, creating it", properties.bucket());
                s3Client.createBucket(b -> b.bucket(properties.bucket()));
                log.info("S3 bucket '{}' created", properties.bucket());
            }
        };
    }

    /**
     * Check if AWS credentials are properly configured.
     *
     * @return true if both access key and secret key are provided and non-empty
     */
    private boolean hasValidCredentials() {
        return !isBlank(properties.accessKey()) && !isBlank(properties.secretKey());
    }

    private boolean hasAnyStaticCredential() {
        return !isBlank(properties.accessKey()) || !isBlank(properties.secretKey());
    }

    /**
     * Preserve AWS session credentials when the caller uses STS, IAM Identity Center,
     * or another short-lived credential source exposed as environment variables.
     */
    private StaticCredentialsProvider staticCredentialsProvider() {
        if (!isBlank(properties.sessionToken())) {
            return StaticCredentialsProvider.create(
                AwsSessionCredentials.create(
                    properties.accessKey(), properties.secretKey(), properties.sessionToken()
                )
            );
        }
        return StaticCredentialsProvider.create(
            AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())
        );
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
