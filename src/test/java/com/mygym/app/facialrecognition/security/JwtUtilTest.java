package com.mygym.app.facialrecognition.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String testSecret = "superSecretKeyForJwtSigningThatIsAtLeast256BitsLongForHMACSHA256!";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "encryptionSecret", testSecret);
    }

    @Test
    @DisplayName("Should successfully generate token and extract valid username and role claims")
    void testGenerateTokenAndExtractClaims_Success() {
        String username = "kiosk_admin";
        String role = "ADMIN";

        String token = jwtUtil.generateToken(username, role);

        assertNotNull(token);
        assertFalse(token.isBlank());

        String extractedUsername = jwtUtil.extractUsername(token);
        Claims claims = jwtUtil.extractAllClaims(token);

        assertEquals(username, extractedUsername);
        assertEquals(role, claims.get("role", String.class));
    }

    @Test
    @DisplayName("Should validate token successfully for matching username and unexpired token")
    void testValidateToken_ValidToken_ReturnsTrue() {
        String username = "member_user";
        String role = "MEMBER";
        String token = jwtUtil.generateToken(username, role);

        boolean isValid = jwtUtil.validateToken(token, username);

        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should fail token validation when username does not match token subject")
    void testValidateToken_MismatchedUsername_ReturnsFalse() {
        String token = jwtUtil.generateToken("user_x", "MEMBER");

        boolean isValid = jwtUtil.validateToken(token, "user_y");

        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should throw SignatureException when attempting to parse tampered token")
    void testExtractClaims_TamperedToken_ThrowsException() {
        String token = jwtUtil.generateToken("valid_user", "MEMBER");
        String tamperedToken = token + "bad_signature";

        assertThrows(SignatureException.class, () -> jwtUtil.extractAllClaims(tamperedToken));
    }

    @Test
    @DisplayName("Should return false for isTokenExpired when token is recently issued")
    void testIsTokenExpired_FreshToken_ReturnsFalse() {
        String token = jwtUtil.generateToken("fresh_kiosk", "ADMIN");

        boolean isExpired = jwtUtil.isTokenExpired(token);

        assertFalse(isExpired);
    }
}