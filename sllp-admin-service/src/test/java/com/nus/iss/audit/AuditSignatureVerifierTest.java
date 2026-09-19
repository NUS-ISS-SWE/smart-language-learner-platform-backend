package com.nus.iss.audit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

class AuditSignatureVerifierTest {
    private static final String SECRET = "audit-test-secret";

    @Test
    void acceptsAValidCurrentSignature() throws Exception {
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        String body = "{\"service\":\"test\"}";
        assertDoesNotThrow(() -> new AuditSignatureVerifier(new AuditProperties(SECRET)).verify(timestamp, signature(timestamp, body), body));
    }

    @Test
    void rejectsTamperedPayload() throws Exception {
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        String body = "{\"service\":\"test\"}";
        assertThrows(SecurityException.class, () -> new AuditSignatureVerifier(new AuditProperties(SECRET)).verify(timestamp, signature(timestamp, body), "{\"service\":\"other\"}"));
    }

    private static String signature(String timestamp, String body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return java.util.HexFormat.of().formatHex(mac.doFinal((timestamp + "." + body).getBytes(StandardCharsets.UTF_8)));
    }
}
