package com.orderlifecycle.ingress_service.JwtTests;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.orderlifecycle.ingress_service.security.JwtProvider;

import io.jsonwebtoken.Jwts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("JwtProvider Cryptographic and Token Lifecycle Tests")
public class JwtProviderTest {

    private static final String SECRET_KEY_STRING = "my-test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256";
    private static final long EXPIRATION_TIME_MS = 3600000L; // 1 hour
    private static final String DEFAULT_USER_ID = "test-user-123";

    private JwtProvider jwtProvider;
    private SecretKey secretKey;

    @BeforeEach
    public void setup() {
        this.jwtProvider = new JwtProvider(SECRET_KEY_STRING, EXPIRATION_TIME_MS);
        this.secretKey = new SecretKeySpec(SECRET_KEY_STRING.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Test
    @DisplayName("Should return false when token has null expiration date (claims.getExpiration() == null)")
    public void isValidToken_whenTokenHasNoExpirationDate_returnsFalse() {
        // Arrange: build a token omitting the expiration claim
        String tokenWithoutExpiration = Jwts.builder()
                .subject(DEFAULT_USER_ID)
                .issuedAt(new Date())
                .signWith(secretKey)
                .compact();

        // Act
        Boolean isValid = jwtProvider.isValidToken(tokenWithoutExpiration);

        // Assert
        assertFalse(isValid, "Token with missing/null expiration claim must be rejected as invalid");
    }

    @Test
    @DisplayName("Should return true when token is valid and not expired")
    public void isValidToken_whenTokenIsValid_returnsTrue() {
        // Arrange
        String validToken = jwtProvider.generateToken(DEFAULT_USER_ID);

        // Act
        Boolean isValid = jwtProvider.isValidToken(validToken);

        // Assert
        assertTrue(isValid, "Legitimately generated token within expiration period must be valid");
    }

    @Test
    @DisplayName("Should return false when token is expired")
    public void isValidToken_whenTokenIsExpired_returnsFalse() {
        // Arrange: build an expired token
        Date pastDate = new Date(System.currentTimeMillis() - 10000);
        String expiredToken = Jwts.builder()
                .subject(DEFAULT_USER_ID)
                .issuedAt(new Date(System.currentTimeMillis() - 20000))
                .expiration(pastDate)
                .signWith(secretKey)
                .compact();

        // Act
        Boolean isValid = jwtProvider.isValidToken(expiredToken);

        // Assert
        assertFalse(isValid, "Expired token must be rejected as invalid");
    }

    @Test
    @DisplayName("Should return false when token is signed with a different key")
    public void isValidToken_whenTokenSignatureIsInvalid_returnsFalse() {
        // Arrange: token signed with a different secret
        SecretKey foreignKey = new SecretKeySpec(
                "another-unrelated-secret-key-that-is-at-least-256-bits-long".getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        String tamperedToken = Jwts.builder()
                .subject(DEFAULT_USER_ID)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(foreignKey)
                .compact();

        // Act
        Boolean isValid = jwtProvider.isValidToken(tamperedToken);

        // Assert
        assertFalse(isValid, "Token signed with an unrecognized key must be rejected");
    }

    @Test
    @DisplayName("Should extract subject as userId from valid token")
    public void extractUserId_returnsSubjectFromToken() {
        // Arrange
        String token = jwtProvider.generateToken(DEFAULT_USER_ID);

        // Act
        String extractedUserId = jwtProvider.extractUserId(token);

        // Assert
        assertEquals(DEFAULT_USER_ID, extractedUserId, "Extracted userId must match the token subject");
    }
}
