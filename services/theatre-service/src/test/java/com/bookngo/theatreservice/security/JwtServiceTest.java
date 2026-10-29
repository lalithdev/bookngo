package com.bookngo.theatreservice.security;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private JwtService jwtService;
    private final String secret = "test-secret-key-for-theatre-service-at-least-32-chars-long";
    private final long expirationMs = 3600000;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(secret, expirationMs);
    }

    @Test
    void testGenerateAndValidateToken() {
        UUID userId = UUID.randomUUID();
        List<String> roles = List.of("THEATRE_OPERATOR");

        String token = jwtService.generateToken(userId, roles);

        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));
        assertEquals(userId, jwtService.extractUserId(token));
        assertEquals(roles, jwtService.extractRoles(token));
    }

    @Test
    void testValidateInvalidToken() {
        assertFalse(jwtService.validateToken("invalid.jwt.token"));
    }

    @Test
    void testExpiredToken() {
        JwtService shortLivedJwtService = new JwtService(secret, -10000);
        UUID userId = UUID.randomUUID();
        String token = shortLivedJwtService.generateToken(userId, List.of("THEATRE_OPERATOR"));

        assertFalse(shortLivedJwtService.validateToken(token));
    }

    @Test
    void testMissingOrBlankSecretThrowsException() {
        assertThrows(IllegalStateException.class, () -> new JwtService(null, expirationMs));
        assertThrows(IllegalStateException.class, () -> new JwtService("", expirationMs));
        assertThrows(IllegalStateException.class, () -> new JwtService("   ", expirationMs));
    }

    @Test
    void testShortSecretThrowsException() {
        assertThrows(IllegalStateException.class, () -> new JwtService("short-key-under-32-bytes", expirationMs));
    }

    @Test
    void testTokenSignedWithDifferentKeyFailsValidation() {
        String differentSecret = "AnotherSecretKeyForTestingPurposesOnlyMustBeAtLeast32BytesLong!";
        JwtService otherJwtService = new JwtService(differentSecret, expirationMs);
        String token = otherJwtService.generateToken(UUID.randomUUID(), List.of("THEATRE_OPERATOR"));

        assertFalse(jwtService.validateToken(token));
    }
}

