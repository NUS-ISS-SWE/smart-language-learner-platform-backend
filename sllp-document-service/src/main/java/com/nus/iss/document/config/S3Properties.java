package com.nus.iss.document.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Document-storage settings shared by the S3 client configuration and service.
 */
@ConfigurationProperties(prefix = "aws.s3")
public record S3Properties(
    String region,
    String accessKey,
    String secretKey,
    String sessionToken,
    String endpoint,
    String publicEndpoint,
    String bucket
) {
}
