package com.trackforge.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Generate JWT with additional claims.
     */
    public String generateToken(
            UserDetails userDetails,
            Map<String, Object> extraClaims
    ) {

        Map<String, Object> claims =
                extraClaims != null
                        ? new HashMap<>(extraClaims)
                        : new HashMap<>();

        Date now = new Date();

        Date expiry =
                new Date(
                        now.getTime() + expiration
                );

        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getKey())
                .compact();
    }

    /**
     * Generate JWT without additional claims.
     */
    public String generateToken(
            UserDetails userDetails
    ) {
        return generateToken(
                userDetails,
                new HashMap<>()
        );
    }

    /**
     * Extract username / subject from JWT.
     */
    public String extractUsername(
            String token
    ) {

        try {
            return getClaims(token).getSubject();

        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Validate JWT against the supplied user.
     */
    public boolean isTokenValid(
            String token,
            UserDetails userDetails
    ) {

        if (token == null ||
                token.isBlank() ||
                userDetails == null) {
            return false;
        }

        try {

            Claims claims = getClaims(token);

            String username = claims.getSubject();

            if (username == null ||
                    !username.equals(
                            userDetails.getUsername()
                    )) {
                return false;
            }

            Date expirationDate =
                    claims.getExpiration();

            if (expirationDate == null) {
                return false;
            }

            return expirationDate.after(
                    new Date()
            );

        } catch (JwtException |
                 IllegalArgumentException e) {

            return false;
        }
    }

    /**
     * Check whether a token has expired.
     */
    public boolean isExpired(
            String token
    ) {

        try {

            Date expirationDate =
                    getClaims(token).getExpiration();

            return expirationDate == null ||
                    expirationDate.before(
                            new Date()
                    );

        } catch (JwtException |
                 IllegalArgumentException e) {

            return true;
        }
    }

    /**
     * Parse and verify JWT claims.
     */
    private Claims getClaims(
            String token
    ) {

        if (token == null ||
                token.isBlank()) {
            throw new IllegalArgumentException(
                    "JWT token cannot be null or empty"
            );
        }

        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * JWT expiration duration in milliseconds.
     */
    public long getExpiration() {
        return expiration;
    }
}