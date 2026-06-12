package com.divyagems.ecommerce.security.jwt;

import com.divyagems.ecommerce.security.UserDetailsImpl;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Utility component for generating, parsing, and validating JWTs.
 *
 * Algorithm  : HS256
 * Claims     : sub (email), roles, jti (UUID), iat, exp
 * Secret key : Base64-encoded value from ${app.jwt.secret}
 *
 * The JWT secret MUST be a Base64-encoded string of at least 256 bits (32 bytes)
 * for HS256 to be secure. Example: openssl rand -base64 64
 */
@Slf4j
@Component
public class JwtUtils {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    // ─── Token Generation ──────────────────────────────────────

    /**
     * Generate a signed JWT access token for the given UserDetails.
     * Subject is the user's email; roles claim holds all authorities.
     */
    public String generateAccessToken(UserDetails userDetails) {
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .claim("roles", roles)
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Generate a cryptographically random opaque refresh token string.
     * The actual token is stored in the DB and referenced by the client.
     */
    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    // ─── Token Parsing ─────────────────────────────────────────

    /** Extract the email (subject) from a token. */
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    /** Extract and return all claims from a JWT. */
    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // ─── Token Validation ──────────────────────────────────────

    /**
     * Validate a JWT: checks signature integrity and expiry.
     *
     * @return true if the token is valid and non-expired
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /** Return the configured access token lifetime in seconds. */
    public long getExpirationInSeconds() {
        return jwtExpirationMs / 1000;
    }

    // ─── Private Helpers ───────────────────────────────────────

    /**
     * Decode the Base64 JWT secret and build an HMAC-SHA key.
     * The secret from environment should be a Base64-encoded string of ≥32 bytes.
     */
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
