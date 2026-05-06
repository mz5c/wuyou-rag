package com.wuyou.rag.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expirationMs;
    private final StringRedisTemplate redisTemplate;

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs,
            StringRedisTemplate redisTemplate) {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
        this.redisTemplate = redisTemplate;
    }

    public String generateToken(Long userId, String username, String role) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Validate token and return claims atomically (single parse).
     */
    public Optional<Claims> validateAndParse(String token) {
        try {
            String blacklistKey = BLACKLIST_PREFIX + hashToken(token);
            if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
                log.warn("Token is blacklisted");
                return Optional.empty();
            }
            return Optional.of(parseToken(token));
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Add token to Redis blacklist. TTL is derived from the token's remaining expiration.
     */
    public void blacklistToken(String token) {
        try {
            Claims claims = parseToken(token);
            long remainingMs = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (remainingMs <= 0) {
                return;
            }
            String blacklistKey = BLACKLIST_PREFIX + hashToken(token);
            redisTemplate.opsForValue().set(blacklistKey, "1", remainingMs, TimeUnit.MILLISECONDS);
            log.info("Token blacklisted for {} ms", remainingMs);
        } catch (Exception e) {
            log.warn("Could not blacklist token: {}", e.getMessage());
        }
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
