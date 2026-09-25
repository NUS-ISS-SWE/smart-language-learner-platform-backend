package com.nus.iss.document.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class S3ConfigTest {

    @Test
    void allowsWorkloadCredentialProviderWhenStaticCredentialsAreAbsent() {
        S3Config config = new S3Config(properties("", "", ""));

        assertDoesNotThrow(config::validateRequiredConfiguration);
    }

    @Test
    void rejectsPartialStaticCredentials() {
        S3Config config = new S3Config(properties("access-key", "", ""));

        assertThrows(IllegalStateException.class, config::validateRequiredConfiguration);
    }

    @Test
    void rejectsSessionTokenWithoutStaticCredentials() {
        S3Config config = new S3Config(properties("", "", "session-token"));

        assertThrows(IllegalStateException.class, config::validateRequiredConfiguration);
    }

    private S3Properties properties(String accessKey, String secretKey, String sessionToken) {
        return new S3Properties(
            "ap-southeast-1", accessKey, secretKey, sessionToken, "", "", "sllp-uploads-test"
        );
    }
}
