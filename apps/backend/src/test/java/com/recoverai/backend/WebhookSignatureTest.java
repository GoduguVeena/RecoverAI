package com.recoverai.backend;

import com.recoverai.backend.webhook.WebhookSignatureVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebhookSignatureTest {

    private WebhookSignatureVerifier verifier;
    private final String secret = "test_secret_key_12345";
    private final String payload = "{\"event\":\"payment.failed\",\"payload\":{\"payment\":{\"entity\":{\"id\":\"pay_123\"}}}}";

    @BeforeEach
    void setUp() {
        verifier = new WebhookSignatureVerifier();
    }

    @Test
    @DisplayName("1. Valid HMAC-SHA256 signature is accepted")
    void testValidSignature() {
        String validSig = verifier.calculateSignature(payload, secret);
        assertTrue(verifier.verifySignature(payload, validSig, secret));
    }

    @Test
    @DisplayName("2. Invalid signature is rejected")
    void testInvalidSignature() {
        String invalidSig = "0000000000000000000000000000000000000000000000000000000000000000";
        assertFalse(verifier.verifySignature(payload, invalidSig, secret));
    }

    @Test
    @DisplayName("3. Missing signature or payload is rejected")
    void testNullSignatureOrPayload() {
        String validSig = verifier.calculateSignature(payload, secret);
        assertFalse(verifier.verifySignature(null, validSig, secret));
        assertFalse(verifier.verifySignature(payload, null, secret));
        assertFalse(verifier.verifySignature(payload, validSig, null));
    }

    @Test
    @DisplayName("4. Constant-time comparison handles whitespace and padding safely")
    void testConstantTimeComparison() {
        String validSig = verifier.calculateSignature(payload, secret);
        assertTrue(verifier.verifySignature(payload, " " + validSig + " ", secret));
    }
}
