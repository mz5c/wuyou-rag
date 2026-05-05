package com.wuyou.rag.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
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

    /**
     * Generate JWT token with userId, username, and role claims.
     */
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

    /**
     * Parse and return claims from a JWT token.
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Validate token: check blacklist in Redis first, then verify signature and expiration.
     */
    public boolean validateToken(String token) {
        try {
            String blacklistKey = BLACKLIST_PREFIX + token;
            Boolean isBlacklisted = redisTemplate.hasKey(blacklistKey);
            if (Boolean.TRUE.equals(isBlacklisted)) {
                log.warn("Token is blacklisted");
                return false;
            }
            parseToken(token);
            return true;
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Add token to Redis blacklist with the given TTL.
     */
    public void blacklistToken(String token, long expirationMs) {
        String blacklistKey = BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(blacklistKey, "1", expirationMs, TimeUnit.MILLISECONDS);
        log.info("Token blacklisted");
    }

    /**
     * Extract userId claim from token.
     */
    public Long getUserIdFromToken(String token) {
        return parseToken(token).get("userId", Long.class);
    }

    /**
     * Extract username (subject) from token.
     */
    public String getUsernameFromToken(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * Extract role claim from token.
     */
    public String getRoleFromToken(String token) {
        return parseToken(token).get("role", String.class);
    }
}
