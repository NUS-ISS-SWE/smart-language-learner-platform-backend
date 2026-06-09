package com.nus.iss.document.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * AWS S3 configuration for document storage.
 */
@Configuration
@RequiredArgsConstructor
public class S3Config {

    @Value("${aws.s3.region}")
    private String awsRegion;

    @Value("${aws.s3.access-key}")
    private String accessKey;

    @Value("${aws.s3.secret-key}")
    private String secretKey;

    /**
     * Create S3 client bean.
     *
     * @return S3Client
     */
    @Bean
    public S3Client s3Client() {
        var builder = S3Client.builder().region(Region.of(awsRegion));
        applyCredentials(builder);
        return builder.build();
    }

    /**
     * Create S3 presigner bean for generating presigned URLs.
     *
     * @return S3Presigner
     */
    @Bean
    public S3Presigner s3Presigner() {
        var builder = S3Presigner.builder().region(Region.of(awsRegion));
        applyCredentials(builder);
        return builder.build();
    }

    /**
     * Apply AWS credentials to client builder if provided.
     * Uses static credentials if configured, otherwise falls back to default provider (IAM roles, environment variables).
     *
     * @param builder the AWS SDK builder
     */
    private void applyCredentials(Object builder) {
        if (hasValidCredentials()) {
            AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(accessKey, secretKey);
            if (builder instanceof S3Client.Builder s3Builder) {
                s3Builder.credentialsProvider(StaticCredentialsProvider.create(awsCredentials));
            } else if (builder instanceof S3Presigner.Builder presignerBuilder) {
                presignerBuilder.credentialsProvider(StaticCredentialsProvider.create(awsCredentials));
            }
        }
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
