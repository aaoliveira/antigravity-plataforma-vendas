package com.sellerintelligence.modules.competitorresearch.service;

import com.sellerintelligence.modules.marketplaceadapters.exception.RateLimitExceededException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limitador de taxa em janela deslizante (Sliding Window) para buscas de mercado por tenant e fonte.
 * Garante proteção contra esgotamento de cotas de APIs públicas ou oficiais.
 */
@Component
public class MarketSearchRateLimiter {

    private final Map<String, Deque<Long>> requestWindows = new ConcurrentHashMap<>();

    /**
     * Valida se a requisição é permitida. Se permitida, consome 1 crédito e retorna créditos restantes.
     * Caso contrário, lança RateLimitExceededException.
     */
    public synchronized int checkAndConsume(UUID tenantId, String sourcePolicyId, int limitPerMinute) {
        if (limitPerMinute <= 0) {
            return 0;
        }

        String key = tenantId.toString() + ":" + sourcePolicyId;
        long now = System.currentTimeMillis();
        long windowStart = now - 60_000L;

        Deque<Long> timestamps = requestWindows.computeIfAbsent(key, k -> new ArrayDeque<>());

        // Remove timestamps mais antigos que 1 minuto
        while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= limitPerMinute) {
            long oldest = timestamps.peekFirst();
            long waitMillis = Math.max(50, 60_000L - (now - oldest));
            throw new RateLimitExceededException(
                    sourcePolicyId,
                    String.format("Limite de taxa excedido para o tenant no canal '%s' (%d req/min). Tente novamente em %d segundos.",
                            sourcePolicyId, limitPerMinute, Math.max(1, waitMillis / 1000)),
                    Duration.ofMillis(waitMillis)
            );
        }

        timestamps.addLast(now);
        return Math.max(0, limitPerMinute - timestamps.size());
    }

    public synchronized int getRemaining(UUID tenantId, String sourcePolicyId, int limitPerMinute) {
        if (limitPerMinute <= 0) {
            return 0;
        }
        String key = tenantId.toString() + ":" + sourcePolicyId;
        long now = System.currentTimeMillis();
        long windowStart = now - 60_000L;

        Deque<Long> timestamps = requestWindows.get(key);
        if (timestamps == null) {
            return limitPerMinute;
        }

        // Limpa timestamps antigos
        while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
            timestamps.pollFirst();
        }

        return Math.max(0, limitPerMinute - timestamps.size());
    }

    public synchronized void resetForTest() {
        requestWindows.clear();
    }
}
