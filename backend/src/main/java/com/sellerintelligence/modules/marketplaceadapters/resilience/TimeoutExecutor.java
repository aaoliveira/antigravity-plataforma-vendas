package com.sellerintelligence.modules.marketplaceadapters.resilience;

import com.sellerintelligence.modules.marketplaceadapters.exception.MarketplaceTimeoutException;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Executor delimitado por tempo de requisição (Timeout).
 * Garante que chamadas lentas sejam canceladas para liberar recursos de rede e threads.
 */
public class TimeoutExecutor {

    private final ExecutorService executor;
    private final boolean ownsExecutor;

    public TimeoutExecutor() {
        this(Executors.newVirtualThreadPerTaskExecutor(), true);
    }

    public TimeoutExecutor(ExecutorService executor, boolean ownsExecutor) {
        this.executor = executor;
        this.ownsExecutor = ownsExecutor;
    }

    /**
     * Executa a ação garantindo que termine dentro do prazo configurado.
     */
    public <R> R executeWithTimeout(Callable<R> action, Duration timeout, String marketplaceId) throws Exception {
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            return action.call();
        }

        CompletableFuture<R> future = CompletableFuture.supplyAsync(() -> {
            try {
                return action.call();
            } catch (Exception ex) {
                throw new CompletionExceptionWrapper(ex);
            }
        }, executor);

        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            future.cancel(true);
            throw new MarketplaceTimeoutException(marketplaceId, timeout);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof CompletionExceptionWrapper wrapper) {
                throw wrapper.originalException;
            }
            if (cause instanceof Exception exceptionCause) {
                throw exceptionCause;
            }
            throw new RuntimeException(cause);
        }
    }

    private static class CompletionExceptionWrapper extends RuntimeException {
        private final Exception originalException;

        CompletionExceptionWrapper(Exception originalException) {
            this.originalException = originalException;
        }
    }
}
