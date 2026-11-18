package com.sellerintelligence.modules.marketplaceadapters.resilience;

import com.sellerintelligence.modules.marketplaceadapters.exception.CircuitBreakerOpenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Padrão Circuit Breaker com máquina de estados atômica (CLOSED, OPEN, HALF_OPEN).
 * Interrompe chamadas automaticamente quando o número de falhas consecutivas excede o limiar,
 * protegendo o canal contra suspensão e evitando sobrecarga em parceiros com instabilidade.
 */
public class CircuitBreaker {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreaker.class);

    public enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    private final String marketplaceId;
    private final int failureThreshold;
    private final Duration resetTimeout;

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong lastStateChangeTime = new AtomicLong(System.currentTimeMillis());

    public CircuitBreaker(String marketplaceId, int failureThreshold, Duration resetTimeout) {
        if (failureThreshold <= 0) {
            throw new IllegalArgumentException("failureThreshold deve ser maior que zero");
        }
        if (resetTimeout == null || resetTimeout.isNegative() || resetTimeout.isZero()) {
            throw new IllegalArgumentException("resetTimeout deve ser positivo");
        }
        this.marketplaceId = marketplaceId;
        this.failureThreshold = failureThreshold;
        this.resetTimeout = resetTimeout;
    }

    /**
     * Verifica se o circuito permite a execução da requisição.
     * Lança CircuitBreakerOpenException se o circuito estiver no estado OPEN.
     */
    public void checkPermission() {
        State current = state.get();

        if (current == State.OPEN) {
            long elapsed = System.currentTimeMillis() - lastStateChangeTime.get();
            if (elapsed >= resetTimeout.toMillis()) {
                if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                    lastStateChangeTime.set(System.currentTimeMillis());
                    log.info("[{}] Circuit Breaker transitou de OPEN para HALF_OPEN (teste operacional permitido).", marketplaceId);
                    return;
                }
            }
            throw new CircuitBreakerOpenException(marketplaceId);
        }
    }

    /**
     * Registra o sucesso de uma requisição.
     */
    public void recordSuccess() {
        consecutiveFailures.set(0);
        State previous = state.getAndSet(State.CLOSED);
        if (previous != State.CLOSED) {
            lastStateChangeTime.set(System.currentTimeMillis());
            log.info("[{}] Circuit Breaker recuperado para o estado CLOSED.", marketplaceId);
        }
    }

    /**
     * Registra a falha de uma requisição.
     */
    public void recordFailure(Throwable cause) {
        int failures = consecutiveFailures.incrementAndGet();
        State current = state.get();

        if (current == State.HALF_OPEN || failures >= failureThreshold) {
            if (state.compareAndSet(current, State.OPEN)) {
                lastStateChangeTime.set(System.currentTimeMillis());
                log.warn("[{}] Circuit Breaker ABRIU (estado OPEN) após {} falhas consecutivas. Motivo: {}",
                        marketplaceId, failures, cause != null ? cause.getMessage() : "Desconhecido");
            }
        }
    }

    public State getState() {
        return state.get();
    }

    public int getConsecutiveFailures() {
        return consecutiveFailures.get();
    }

    public void reset() {
        consecutiveFailures.set(0);
        state.set(State.CLOSED);
        lastStateChangeTime.set(System.currentTimeMillis());
    }
}
