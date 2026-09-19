package com.nus.iss.audit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

@Component
class AuditSignatureVerifier {
    private final AuditProperties properties;

    AuditSignatureVerifier(AuditProperties properties) { this.properties = properties; }

    void verify(String timestamp, String signature, String body) {
        try {
            long epoch = Long.parseLong(timestamp);
            if (Math.abs(Instant.now().getEpochSecond() - epoch) > 300) throw new SecurityException("expired audit signature");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.ingestSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = java.util.HexFormat.of().formatHex(mac.doFinal((timestamp + "." + body).getBytes(StandardCharsets.UTF_8)));
            if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII), signature.getBytes(StandardCharsets.US_ASCII))) {
                throw new SecurityException("invalid audit signature");
            }
        } catch (NumberFormatException error) {
            throw new SecurityException("invalid audit timestamp", error);
        } catch (SecurityException error) {
            throw error;
        } catch (Exception error) {
            throw new SecurityException("could not verify audit signature", error);
        }
    }
}
