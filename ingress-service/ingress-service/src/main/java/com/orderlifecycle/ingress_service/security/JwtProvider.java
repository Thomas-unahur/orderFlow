package com.orderlifecycle.ingress_service.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

@Component
public class JwtProvider {
    private final SecretKey secretKey;
    private final long expirationTimeMs;

    public JwtProvider(@Value("${security.jwt.secret-key}") String secretKeyString,
            @Value("${security.jwt.expiration-time-ms}") long expirationTimeMs) {
        this.secretKey = new SecretKeySpec(secretKeyString.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.expirationTimeMs = expirationTimeMs;
    }

    public Boolean isValidToken(String token) {
        try {
            Claims claims = getClaims(token);
            Date expiration = claims.getExpiration();
            if (expiration == null) {
                return false;
            }
            return !expiration.before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String extractUserId(String token) {
        return getClaims(token).getSubject();
    }

    public String generateToken(String userId) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expirationTimeMs);

        return Jwts.builder()
                .subject(userId)
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(secretKey)
                .compact();
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
