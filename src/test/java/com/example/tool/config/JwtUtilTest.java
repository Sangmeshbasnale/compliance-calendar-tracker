package com.example.tool.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private static final String SECRET = "test-secret-key-for-unit-tests-must-be-32-chars-long";
    private static final long EXPIRATION = 86400000L;

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, EXPIRATION);
    }

    @Nested
    @DisplayName("generateToken()")
    class GenerateToken {

        @Test
        @DisplayName("should return non-null token")
        void generateToken_returnsNonNull() {
            String token = jwtUtil.generateToken("alice", "ROLE_ADMIN");
            assertNotNull(token);
            assertFalse(token.isBlank());
        }

        @Test
        @DisplayName("should produce a valid JWT with three parts")
        void generateToken_hasThreeParts() {
            String token = jwtUtil.generateToken("alice", "ROLE_ADMIN");
            assertEquals(3, token.split("\\.").length);
        }
    }

    @Nested
    @DisplayName("extractUsername()")
    class ExtractUsername {

        @Test
        @DisplayName("should extract correct username from token")
        void extractUsername_returnsCorrectUsername() {
            String token = jwtUtil.generateToken("alice", "ROLE_ADMIN");
            assertEquals("alice", jwtUtil.extractUsername(token));
        }
    }

    @Nested
    @DisplayName("extractRole()")
    class ExtractRole {

        @Test
        @DisplayName("should extract correct role from token")
        void extractRole_returnsCorrectRole() {
            String token = jwtUtil.generateToken("bob", "ROLE_VIEWER");
            assertEquals("ROLE_VIEWER", jwtUtil.extractRole(token));
        }
    }

    @Nested
    @DisplayName("isValid()")
    class IsValid {

        @Test
        @DisplayName("should return true for a valid token")
        void isValid_validToken_returnsTrue() {
            String token = jwtUtil.generateToken("alice", "ROLE_ADMIN");
            assertTrue(jwtUtil.isValid(token));
        }

        @Test
        @DisplayName("should return false for a tampered token")
        void isValid_tamperedToken_returnsFalse() {
            String token = jwtUtil.generateToken("alice", "ROLE_ADMIN");
            String tampered = token.substring(0, token.length() - 5) + "XXXXX";
            assertFalse(jwtUtil.isValid(tampered));
        }

        @Test
        @DisplayName("should return false for a blank token")
        void isValid_blankToken_returnsFalse() {
            assertFalse(jwtUtil.isValid(""));
        }

        @Test
        @DisplayName("should return false for a random string")
        void isValid_randomString_returnsFalse() {
            assertFalse(jwtUtil.isValid("not.a.jwt"));
        }

        @Test
        @DisplayName("should return false for an expired token")
        void isValid_expiredToken_returnsFalse() {
            // Build a token that expired 1 second ago using the same key
            SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
            String expiredToken = Jwts.builder()
                    .subject("alice")
                    .claim("role", "ROLE_ADMIN")
                    .issuedAt(new Date(System.currentTimeMillis() - 10000))
                    .expiration(new Date(System.currentTimeMillis() - 1000))
                    .signWith(key)
                    .compact();

            assertFalse(jwtUtil.isValid(expiredToken));
        }

        @Test
        @DisplayName("should return false for token signed with different key")
        void isValid_wrongKey_returnsFalse() {
            SecretKey wrongKey = Keys.hmacShaKeyFor(
                    "completely-different-secret-key-32-chars!!".getBytes(StandardCharsets.UTF_8));
            String wrongToken = Jwts.builder()
                    .subject("alice")
                    .expiration(new Date(System.currentTimeMillis() + 86400000))
                    .signWith(wrongKey)
                    .compact();

            assertFalse(jwtUtil.isValid(wrongToken));
        }
    }
}
