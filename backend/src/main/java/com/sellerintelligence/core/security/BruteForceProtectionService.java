package com.sellerintelligence.core.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço de proteção contra ataques de força bruta (Brute Force Protection).
 * Bloqueia temporariamente IPs ou credenciais com falhas consecutivas de autenticação.
 */
@Service
public class BruteForceProtectionService {

    private final int maxAttempts;
    private final long lockoutDurationSeconds;

    private final Map<String, AttemptRecord> attempts = new ConcurrentHashMap<>();

    public BruteForceProtectionService(
            @Value("${security.brute-force.max-attempts:5}") int maxAttempts,
            @Value("${security.brute-force.lockout-duration-seconds:900}") long lockoutDurationSeconds
    ) {
        this.maxAttempts = maxAttempts;
        this.lockoutDurationSeconds = lockoutDurationSeconds;
    }

    public boolean isBlocked(String key) {
        AttemptRecord record = attempts.get(key);
        if (record == null) {
            return false;
        }

        Instant now = Instant.now();
        if (record.lockoutUntil != null) {
            if (now.isBefore(record.lockoutUntil)) {
                return true;
            } else {
                // Período de bloqueio expirou; reseta o registro
                attempts.remove(key);
                return false;
            }
        }

        return false;
    }

    public void recordFailure(String key) {
        Instant now = Instant.now();
        attempts.compute(key, (k, existing) -> {
            if (existing == null || (existing.lockoutUntil != null && now.isAfter(existing.lockoutUntil))) {
                return new AttemptRecord(1, now, null);
            }

            int newCount = existing.failureCount + 1;
            Instant lockout = null;
            if (newCount >= maxAttempts) {
                lockout = now.plusSeconds(lockoutDurationSeconds);
            }
            return new AttemptRecord(newCount, now, lockout);
        });
    }

    public void recordSuccess(String key) {
        attempts.remove(key);
    }

    public long getRemainingLockoutSeconds(String key) {
        AttemptRecord record = attempts.get(key);
        if (record != null && record.lockoutUntil != null) {
            long remaining = record.lockoutUntil.getEpochSecond() - Instant.now().getEpochSecond();
            return Math.max(0, remaining);
        }
        return 0;
    }

    public void reset() {
        attempts.clear();
    }

    private static class AttemptRecord {
        final int failureCount;
        final Instant lastAttempt;
        final Instant lockoutUntil;

        AttemptRecord(int failureCount, Instant lastAttempt, Instant lockoutUntil) {
            this.failureCount = failureCount;
            this.lastAttempt = lastAttempt;
            this.lockoutUntil = lockoutUntil;
        }
    }
}
