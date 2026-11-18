package com.sellerintelligence.modules.marketplaceadapters.resilience;

import com.sellerintelligence.modules.marketplaceadapters.exception.BulkheadFullException;

import java.util.concurrent.Semaphore;

/**
 * Padrão Bulkhead para isolamento de falhas e limitação de chamadas simultâneas por canal.
 * Previne que lentidão ou gargalos em um parceiro saturem todas as threads da aplicação.
 */
public class Bulkhead {

    private final String marketplaceId;
    private final int maxConcurrentCalls;
    private final Semaphore semaphore;

    public Bulkhead(String marketplaceId, int maxConcurrentCalls) {
        if (maxConcurrentCalls <= 0) {
            throw new IllegalArgumentException("maxConcurrentCalls deve ser maior que zero");
        }
        this.marketplaceId = marketplaceId;
        this.maxConcurrentCalls = maxConcurrentCalls;
        this.semaphore = new Semaphore(maxConcurrentCalls, true);
    }

    /**
     * Tenta adquirir um slot de execução concorrente.
     * Se todas as vagas estiverem ocupadas, falha imediatamente com BulkheadFullException (Fail-Fast).
     */
    public void acquire() {
        if (!semaphore.tryAcquire()) {
            throw new BulkheadFullException(marketplaceId, maxConcurrentCalls);
        }
    }

    /**
     * Libera o slot previamente adquirido (deve ser chamado em bloco finally).
     */
    public void release() {
        semaphore.release();
    }

    public int getAvailablePermits() {
        return semaphore.availablePermits();
    }

    public int getMaxConcurrentCalls() {
        return maxConcurrentCalls;
    }
}
