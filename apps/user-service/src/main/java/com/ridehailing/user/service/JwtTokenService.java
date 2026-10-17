package com.ridehailing.user.service;

import com.ridehailing.user.api.AuthController.AuthResponse;
import com.ridehailing.user.domain.Account;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {
    private final String issuer;
    private final String secretKey;
    private final long expirationMs;

    public JwtTokenService(@Value("${security.jwt.issuer}") String issuer,
                           @Value("${security.jwt.secret-key}") String secretKey,
                           @Value("${security.jwt.expiration-ms}") long expirationMs) {
        this.issuer = issuer;
        this.secretKey = secretKey;
        this.expirationMs = expirationMs;
    }

    public AuthResponse issue(Account account, String refreshToken) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("security.jwt.secret-key must be configured");
        }
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMs);
        List<String> roles = account.getRoles().stream().map(Enum::name).sorted().toList();
        String token = Jwts.builder()
                .subject(account.getId().toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .id(java.util.UUID.randomUUID().toString())
                .claim("roles", roles)
                .claim("token_type", "access")
                .signWith(signingKey())
                .compact();
        return new AuthResponse(token, refreshToken, "Bearer", expirationMs / 1000,
                30L * 24 * 60 * 60, account.getId().toString(), roles);
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }
}
