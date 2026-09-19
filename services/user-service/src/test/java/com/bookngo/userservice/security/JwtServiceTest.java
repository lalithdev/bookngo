package com.bookngo.userservice.security;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private JwtService jwtService;
    private final String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final long expirationMs = 3600000;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(secret, expirationMs);
    }

    @Test
    void testGenerateAndValidateToken() {
        UUID userId = UUID.randomUUID();
        List<String> roles = List.of("CUSTOMER");
        String phoneNumber = "+919876543210";

        String token = jwtService.generateToken(userId, roles, phoneNumber);

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
        String token = shortLivedJwtService.generateToken(userId, List.of("CUSTOMER"), "+919876543210");

        assertFalse(shortLivedJwtService.validateToken(token));
    }
}
