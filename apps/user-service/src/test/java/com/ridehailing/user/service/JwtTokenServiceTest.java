package com.ridehailing.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.ridehailing.user.domain.Account;
import com.ridehailing.user.domain.AccountRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Base64;
import java.util.Set;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

class JwtTokenServiceTest {
    private static final String SECRET = Base64.getEncoder().encodeToString(
            "a-test-secret-key-that-is-at-least-32-bytes-long".getBytes());

    @Test
    void issuesCanonicalAccessClaims() {
        JwtTokenService service = new JwtTokenService("micro-rider-user-service", SECRET, 3600000);
        Account account = new Account("rider@example.com", "hash", Set.of(AccountRole.RIDER));

        var response = service.issue(account, "refresh-token");
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(response.accessToken()).getPayload();

        assertNotNull(response.accessToken());
        assertEquals(account.getId().toString(), claims.getSubject());
        assertEquals("micro-rider-user-service", claims.getIssuer());
        assertEquals("access", claims.get("token_type"));
        assertEquals(1, ((java.util.List<?>) claims.get("roles")).size());
    }
}
