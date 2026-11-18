package com.sellerintelligence.modules.marketplaceadapters.resilience;

import com.sellerintelligence.modules.marketplaceadapters.exception.RateLimitExceededException;

import java.time.Duration;

/**
 * Limitador de taxa determinístico baseado no algoritmo Token Bucket.
 * Controla a cadência de requisições enviadas aos marketplaces para respeitar
 * estritamente as quotas oficiais (ex: 10 a 20 req/s no Mercado Livre, 5 req/s na Shopee).
 */
public class RateLimiter {

    private final String marketplaceId;
    private final double capacity;
    private final double refillRatePerSecond;

    private double availableTokens;
    private long lastRefillNanos;

    public RateLimiter(String marketplaceId, double permitsPerSecond) {
        if (permitsPerSecond <= 0) {
            throw new IllegalArgumentException("permitsPerSecond deve ser maior que zero");
        }
        this.marketplaceId = marketplaceId;
        this.capacity = permitsPerSecond;
        this.refillRatePerSecond = permitsPerSecond;
        this.availableTokens = permitsPerSecond;
        this.lastRefillNanos = System.nanoTime();
    }

    /**
     * Tenta adquirir permissão para 1 requisição. Se a quota estiver esgotada,
     * lança RateLimitExceededException informando o tempo estimado de recomposição.
     */
    public synchronized void acquire() {
        refill();

        if (availableTokens >= 1.0) {
            availableTokens -= 1.0;
        } else {
            double missingTokens = 1.0 - availableTokens;
            long waitMillis = (long) Math.ceil((missingTokens / refillRatePerSecond) * 1000.0);
            Duration retryAfter = Duration.ofMillis(Math.max(waitMillis, 50));

            throw new RateLimitExceededException(
                    marketplaceId,
                    String.format("Taxa de requisições excedida no canal '%s'. Disponível=%.2f, Requerido=1.0",
                            marketplaceId, availableTokens),
                    retryAfter
            );
        }
    }

    /**
     * Tenta adquirir sem lançar exceção.
     */
    public synchronized boolean tryAcquire() {
        refill();
        if (availableTokens >= 1.0) {
            availableTokens -= 1.0;
            return true;
        }
        return false;
    }

    private void refill() {
        long now = System.nanoTime();
        double elapsedSeconds = (now - lastRefillNanos) / 1_000_000_000.0;
        if (elapsedSeconds > 0) {
            availableTokens = Math.min(capacity, availableTokens + (elapsedSeconds * refillRatePerSecond));
            lastRefillNanos = now;
        }
    }

    public synchronized double getAvailableTokens() {
        refill();
        return availableTokens;
    }
}
