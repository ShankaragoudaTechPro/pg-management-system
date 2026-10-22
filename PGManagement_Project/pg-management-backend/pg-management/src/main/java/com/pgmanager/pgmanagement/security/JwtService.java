
package com.pgmanager.pgmanagement.security;

import com.pgmanager.pgmanagement.entity.Owner;
import com.pgmanager.pgmanagement.repository.OwnerRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final OwnerRepository ownerRepository;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    // Signing key
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Generate token with role and session version
    public String generateToken(
            String email,
            Role role,
            long sessionVersion) {

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(email)
                .claim("role", role.name())
                .claim("sessionVersion", sessionVersion)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    // Generate token using the current database session version
    public String generateToken(String email, Role role) {
        return generateToken(
                email,
                role,
                getCurrentSessionVersion(email)
        );
    }

    // Backward-compatible owner token generation
    public String generateToken(String email) {
        return generateToken(
                email,
                Role.OWNER,
                getCurrentSessionVersion(email)
        );
    }

    private long getCurrentSessionVersion(String email) {
        Owner owner = ownerRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Owner not found for JWT generation"
                        ));

        return owner.getSessionVersion();
    }

    // Extract email
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    // Extract role
    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    // Extract session version
    public long extractSessionVersion(String token) {
        Claims claims = parseClaims(token);
        Object value = claims.get("sessionVersion");

        if (value == null) {
            return 0L;
        }

        if (!(value instanceof Number)) {
            throw new IllegalArgumentException(
                    "Invalid JWT session version"
            );
        }

        long version = ((Number) value).longValue();

        if (version < 0) {
            throw new IllegalArgumentException(
                    "Invalid JWT session version"
            );
        }

        return version;
    }

    // Parse and verify JWT
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Validate token
    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseClaims(token);
            Date expiry = claims.getExpiration();

            return expiry != null && expiry.after(new Date());

        } catch (Exception exception) {
            return false;
        }
    }
}