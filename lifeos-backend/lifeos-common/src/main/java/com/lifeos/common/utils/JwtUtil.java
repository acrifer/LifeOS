package com.lifeos.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;

public class JwtUtil {
    
    // A secure key for HS256 algorithm (at least 256 bits/32 characters)
    private static final String SECRET_KEY_STR = "LifeOS_Secret_Key_LifeOS_Secret_Key_LifeOS_Secret_Key";
    private static final long EXPIRATION_TIME = 86400000; // 24 hours

    private static Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY_STR.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate token
     */
    public static String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + EXPIRATION_TIME);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("username", username)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Parse token
     */
    public static Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null; // Invalid token
        }
    }

    /**
     * Get userId from token
     */
    public static Long getUserId(String token) {
        Claims claims = parseToken(token);
        if (claims != null && claims.getSubject() != null) {
            return Long.parseLong(claims.getSubject());
        }
        return null;
    }
    
    /**
     * Validate Auth header format
     */
    public static String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return authHeader;
    }
}
