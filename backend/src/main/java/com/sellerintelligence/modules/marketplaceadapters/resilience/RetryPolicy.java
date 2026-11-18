package com.sellerintelligence.modules.marketplaceadapters.resilience;

import com.sellerintelligence.modules.marketplaceadapters.exception.CircuitBreakerOpenException;
import com.sellerintelligence.modules.marketplaceadapters.exception.KillSwitchActivatedException;
import com.sellerintelligence.modules.marketplaceadapters.exception.RateLimitExceededException;
import com.sellerintelligence.modules.marketplaceadapters.exception.RetryExhaustedException;
import com.sellerintelligence.modules.marketplaceadapters.exception.UnsupportedCapabilityException;
import com.sellerintelligence.modules.marketplaceadapters.exception.UnverifiedCapabilityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Política de retentativas com algoritmo Exponential Backoff com Full Jitter
 * e respeito estrito ao tempo informado pelo cabeçalho HTTP Retry-After.
 *
 * Fórmula: Sleep = random(0, min(maxDelay, baseDelay * 2^attempt))
 */
public class RetryPolicy {

    private static final Logger log = LoggerFactory.getLogger(RetryPolicy.class);

    private final int maxAttempts;
    private final Duration baseDelay;
    private final Duration maxDelay;

    public RetryPolicy(int maxAttempts, Duration baseDelay, Duration maxDelay) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts deve ser pelo menos 1");
        }
        if (baseDelay == null || baseDelay.isNegative() || baseDelay.isZero()) {
            throw new IllegalArgumentException("baseDelay deve ser positivo");
        }
        if (maxDelay == null || maxDelay.compareTo(baseDelay) < 0) {
            throw new IllegalArgumentException("maxDelay deve ser maior ou igual a baseDelay");
        }
        this.maxAttempts = maxAttempts;
        this.baseDelay = baseDelay;
        this.maxDelay = maxDelay;
    }

    /**
     * Executa a tarefa com a política de retentativas configurada.
     */
    public <R> R execute(Callable<R> action, String marketplaceId) throws Exception {
        int attempt = 0;
        Throwable lastError = null;

        while (attempt < maxAttempts) {
            attempt++;
            try {
                return action.call();
            } catch (Throwable ex) {
                lastError = ex;

                // Falha imediata (Fail-Fast): não retentar erros de negócio, políticas ou kill switch
                if (isNonRetryable(ex)) {
                    log.debug("[{}] Erro não retentável: {}", marketplaceId, ex.getMessage());
                    throw ex;
                }

                if (attempt >= maxAttempts) {
                    break;
                }

                long backoffMillis = calculateBackoff(attempt, ex);
                log.info("[{}] Tentativa {}/{} falhou ({}). Aguardando {} ms com Full Jitter antes da próxima tentativa.",
                        marketplaceId, attempt, maxAttempts, ex.getClass().getSimpleName(), backoffMillis);

                sleep(backoffMillis);
            }
        }

        throw new RetryExhaustedException(marketplaceId, maxAttempts, lastError);
    }

    /**
     * Calcula o tempo de espera utilizando Full Jitter ou o tempo do Retry-After se fornecido.
     */
    public long calculateBackoff(int attempt, Throwable error) {
        if (error instanceof RateLimitExceededException rateLimitEx && rateLimitEx.getRetryAfter() != null) {
            long retryAfterMs = rateLimitEx.getRetryAfter().toMillis();
            // Aplica jitter aleatório de +0% a +20% sobre o tempo exigido pelo canal
            long jitter = ThreadLocalRandom.current().nextLong(0, Math.max(1, (long) (retryAfterMs * 0.2)));
            return retryAfterMs + jitter;
        }

        long exponentialCap = Math.min(
                maxDelay.toMillis(),
                (long) (baseDelay.toMillis() * Math.pow(2, attempt - 1))
        );

        if (exponentialCap <= 0) {
            return 0;
        }

        return ThreadLocalRandom.current().nextLong(0, exponentialCap + 1);
    }

    private boolean isNonRetryable(Throwable ex) {
        return ex instanceof KillSwitchActivatedException
                || ex instanceof UnverifiedCapabilityException
                || ex instanceof UnsupportedCapabilityException
                || ex instanceof CircuitBreakerOpenException;
    }

    protected void sleep(long millis) {
        if (millis <= 0) return;
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrompida durante backoff de retry", e);
        }
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public Duration getBaseDelay() {
        return baseDelay;
    }

    public Duration getMaxDelay() {
        return maxDelay;
    }
}
