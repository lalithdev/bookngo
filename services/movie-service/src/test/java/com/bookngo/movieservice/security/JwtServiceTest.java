package com.bookngo.movieservice.security;

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
    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION_MS = 3600000;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, EXPIRATION_MS);
    }

    @Test
    void testGenerateAndValidateToken_Success() {
        UUID userId = UUID.randomUUID();
        List<String> roles = List.of("ADMIN");

        String token = jwtService.generateToken(userId, roles);
        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));
        assertEquals(userId, jwtService.extractUserId(token));

        List<String> extractedRoles = jwtService.extractRoles(token);
        assertNotNull(extractedRoles);
        assertEquals(1, extractedRoles.size());
        assertEquals("ADMIN", extractedRoles.get(0));
    }

    @Test
    void testValidateToken_InvalidToken_ReturnsFalse() {
        assertFalse(jwtService.validateToken("invalid.token.string"));
    }

    @Test
    void testValidateToken_ExpiredToken_ReturnsFalse() {
        JwtService expiredJwtService = new JwtService(SECRET, -1000);
        UUID userId = UUID.randomUUID();
        String token = expiredJwtService.generateToken(userId, List.of("ADMIN"));
        assertFalse(jwtService.validateToken(token));
    }
}
