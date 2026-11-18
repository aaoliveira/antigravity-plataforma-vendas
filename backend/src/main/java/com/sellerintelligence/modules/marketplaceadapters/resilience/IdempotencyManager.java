package com.sellerintelligence.modules.marketplaceadapters.resilience;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Gerenciador de idempotência em memória para evitar duplicação de requisições ou processamento duplo.
 */
@Component
public class IdempotencyManager {

    private record Entry(Object result, long timestamp) {
    }

    private final ConcurrentMap<String, Entry> store = new ConcurrentHashMap<>();
    private final long ttlMillis;

    public IdempotencyManager() {
        this(3600_000L); // 1 hora de TTL por padrão
    }

    public IdempotencyManager(long ttlMillis) {
        this.ttlMillis = ttlMillis;
    }

    /**
     * Verifica se a chave de idempotência já foi processada anteriormente.
     */
    public boolean isProcessed(String key) {
        if (key == null) return false;
        Entry entry = store.get(key);
        if (entry == null) return false;

        if (System.currentTimeMillis() - entry.timestamp > ttlMillis) {
            store.remove(key);
            return false;
        }
        return true;
    }

    /**
     * Registra o resultado da operação sob a chave de idempotência.
     */
    public void record(String key, Object result) {
        if (key == null) return;
        store.put(key, new Entry(result, System.currentTimeMillis()));
    }

    /**
     * Recupera o resultado em cache da operação idempotente.
     */
    @SuppressWarnings("unchecked")
    public <R> R getCachedResult(String key) {
        if (key == null) return null;
        Entry entry = store.get(key);
        if (entry != null && System.currentTimeMillis() - entry.timestamp <= ttlMillis) {
            return (R) entry.result;
        }
        return null;
    }

    public void clear() {
        store.clear();
    }
}
