package com.sellerintelligence.modules.marketplaceconnection.mercadolivre;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Gerenciador de locks distribuídos/concorrentes por conexão de marketplace.
 * Impede que requisições paralelas disparem múltiplos refreshes do mesmo token simultaneamente,
 * evitando a quebra da cadeia de rotação do Mercado Livre (invalid_grant).
 */
@Component
public class DistributedConnectionLock {

    private static final Logger log = LoggerFactory.getLogger(DistributedConnectionLock.class);
    private static final long DEFAULT_LOCK_TIMEOUT_SECONDS = 15;

    private final Map<UUID, ReentrantLock> locks = new ConcurrentHashMap<>();

    /**
     * Executa uma ação crítica de conexão sob lock exclusivo.
     *
     * @param connectionId identificador da conexão
     * @param action ação a ser executada com exclusividade mútua
     * @return resultado da ação
     */
    public <T> T executeWithLock(UUID connectionId, Supplier<T> action) {
        ReentrantLock lock = locks.computeIfAbsent(connectionId, id -> new ReentrantLock(true));

        boolean acquired = false;
        try {
            acquired = lock.tryLock(DEFAULT_LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("Timeout ao adquirir lock concorrente para a conexão {}", connectionId);
                throw new IllegalStateException("Operação concorrente em andamento para esta conta de marketplace. Tente novamente.");
            }

            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Thread interrompida enquanto aguardava lock de conexão", e);
        } finally {
            if (acquired) {
                lock.unlock();
                // Limpeza suave de locks não utilizados
                if (!lock.hasQueuedThreads()) {
                    locks.remove(connectionId, lock);
                }
            }
        }
    }
}
