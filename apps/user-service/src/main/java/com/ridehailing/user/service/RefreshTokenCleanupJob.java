package com.ridehailing.user.service;

import com.ridehailing.user.repository.RefreshTokenRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCleanupJob {
    private final RefreshTokenRepository repository;

    public RefreshTokenCleanupJob(RefreshTokenRepository repository) {
        this.repository = repository;
    }

    @Scheduled(fixedDelayString = "${security.refresh-token.cleanup-ms:3600000}")
    public void deleteExpiredAndRevokedTokens() {
        Instant now = Instant.now();
        repository.deleteByExpiresAtBeforeOrRevokedAtBefore(now, now.minus(1, ChronoUnit.DAYS));
    }
}
