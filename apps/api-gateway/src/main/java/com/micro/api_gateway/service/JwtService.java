package com.micro.api_gateway.service;

import java.util.Date;
import java.util.List;
import java.util.Collections;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    @Value("${security.jwt.secret-key}")
    private String secretKey;

    @Value("${security.jwt.issuer:micro-rider-user-service}")
    private String issuer;

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            // parseSignedClaims validates signature, format, and expiration automatically
            Claims claims = extractAllClaims(token);
            return issuer.equals(claims.getIssuer())
                    && "access".equals(claims.get("token_type", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Changed return type from Key to SecretKey
    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUUID(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        List<String> roles = extractClaim(token, claims -> claims.get("roles", List.class));
        return roles == null ? Collections.emptyList() : List.copyOf(roles);
    }
}
