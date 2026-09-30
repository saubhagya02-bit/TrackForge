package com.trackforge.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtAuthFilter
        extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

    @Value("${jwt.secret}")
    private String secret;

    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh"
    );

    public JwtAuthFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {

        return (exchange, chain) -> {

            String path = exchange.getRequest()
                    .getURI()
                    .getPath();

            // PUBLIC ENDPOINTS
            if (PUBLIC_ENDPOINTS.contains(path)) {
                return chain.filter(exchange);
            }

            // GET AUTHORIZATION HEADER
            String authHeader = exchange.getRequest()
                    .getHeaders()
                    .getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return unauthorized(
                        exchange,
                        "Missing or invalid Authorization header"
                );
            }

            String token = authHeader.substring(7).trim();

            if (token.isEmpty()) {
                return unauthorized(
                        exchange,
                        "Missing JWT token"
                );
            }

            try {

                // VALIDATE JWT
                Claims claims = Jwts.parser()
                        .verifyWith(getKey())
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                // READ CLAIMS
                String userId = getClaimAsString(claims, "userId");
                String username = claims.getSubject();
                String role = getClaimAsString(claims, "role");

                // FORWARD USER INFORMATION
                ServerWebExchange mutatedExchange =
                        exchange.mutate()
                                .request(request -> request.headers(headers -> {

                                    if (userId != null) {
                                        headers.set("X-User-Id", userId);
                                    }

                                    if (username != null) {
                                        headers.set("X-Username", username);
                                    }

                                    if (role != null) {
                                        headers.set("X-User-Role", role);
                                    }

                                }))
                                .build();

                return chain.filter(mutatedExchange);

            } catch (ExpiredJwtException e) {

                return unauthorized(
                        exchange,
                        "Token expired"
                );

            } catch (JwtException e) {

                return unauthorized(
                        exchange,
                        "Invalid token"
                );

            } catch (Exception e) {

                e.printStackTrace();

                return unauthorized(
                        exchange,
                        "JWT authentication failed"
                );
            }
        };
    }

    /**
     * Converts a JWT claim to String safely.
     *
     * This handles claims stored as String, Integer, Long, etc.
     */
    private String getClaimAsString(Claims claims, String claimName) {

        Object value = claims.get(claimName);

        return value != null ? value.toString() : null;
    }

    /**
     * Creates the signing key from the configured JWT secret.
     */
    private SecretKey getKey() {

        return Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Return HTTP 401 when JWT authentication fails.
     */
    private Mono<Void> unauthorized(
            ServerWebExchange exchange,
            String reason
    ) {

        System.out.println(
                "JWT authentication failed: " + reason
        );

        exchange.getResponse()
                .setStatusCode(HttpStatus.UNAUTHORIZED);

        return exchange.getResponse().setComplete();
    }

    public static class Config {
    }
}