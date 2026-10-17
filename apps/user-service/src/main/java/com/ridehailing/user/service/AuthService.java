package com.ridehailing.user.service;

import com.ridehailing.user.api.AuthController.AuthResponse;
import com.ridehailing.user.domain.Account;
import com.ridehailing.user.domain.AccountRole;
import com.ridehailing.user.repository.AccountRepository;
import com.ridehailing.user.repository.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthService(AccountRepository accountRepository, PasswordEncoder passwordEncoder,
                       JwtTokenService jwtTokenService, RefreshTokenRepository refreshTokenRepository) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public AuthResponse register(String rawEmail, String password, String requestedRole) {
        String email = normalizeEmail(rawEmail);
        if (accountRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "AUTH_ACCOUNT_EXISTS");
        }
        AccountRole role = parseRegistrationRole(requestedRole);
        Account account = new Account(email, passwordEncoder.encode(password), Set.of(role));
        accountRepository.save(account);
        return issueWithRefreshToken(account);
    }

    public AuthResponse login(String rawEmail, String password) {
        Account account = accountRepository.findByEmailIgnoreCase(normalizeEmail(rawEmail))
                .filter(Account::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "AUTH_INVALID_CREDENTIALS"));
        if (!passwordEncoder.matches(password, account.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_CREDENTIALS");
        }
        return issueWithRefreshToken(account);
    }

    public AuthResponse refresh(String rawToken) {
        var stored = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> unauthorized("AUTH_INVALID_REFRESH_TOKEN"));
        if (stored.getRevokedAt() != null || stored.getExpiresAt().isBefore(Instant.now())) {
            throw unauthorized("AUTH_INVALID_REFRESH_TOKEN");
        }
        Account account = accountRepository.findById(stored.getAccountId())
                .filter(Account::isEnabled)
                .orElseThrow(() -> unauthorized("AUTH_INVALID_REFRESH_TOKEN"));
        stored.revoke();
        refreshTokenRepository.save(stored);
        return issueWithRefreshToken(account);
    }

    public void logout(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken)).ifPresent(token -> {
            token.revoke();
            refreshTokenRepository.save(token);
        });
    }

    private AuthResponse issueWithRefreshToken(Account account) {
        String rawToken = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
        refreshTokenRepository.save(new com.ridehailing.user.domain.RefreshToken(
                account.getId(), hash(rawToken), Instant.now().plus(30, ChronoUnit.DAYS)));
        return jwtTokenService.issue(account, rawToken);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private ResponseStatusException unauthorized(String code) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, code);
    }

    private AccountRole parseRegistrationRole(String requestedRole) {
        if (requestedRole == null || requestedRole.isBlank()) {
            return AccountRole.RIDER;
        }
        try {
            AccountRole role = AccountRole.valueOf(requestedRole.toUpperCase(Locale.ROOT));
            if (role == AccountRole.ADMIN || role == AccountRole.SYSTEM) {
                throw new IllegalArgumentException();
            }
            return role;
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AUTH_INVALID_ROLE");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
