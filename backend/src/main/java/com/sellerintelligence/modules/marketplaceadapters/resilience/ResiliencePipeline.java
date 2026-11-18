package com.sellerintelligence.modules.marketplaceadapters.resilience;

import com.sellerintelligence.modules.marketplaceadapters.exception.KillSwitchActivatedException;
import com.sellerintelligence.modules.marketplaceadapters.exception.MarketplaceAdapterException;
import com.sellerintelligence.modules.marketplaceadapters.exception.UnsupportedCapabilityException;
import com.sellerintelligence.modules.marketplaceadapters.exception.UnverifiedCapabilityException;
import com.sellerintelligence.modules.marketplaceadapters.model.CapabilityRegistry;
import com.sellerintelligence.modules.marketplaceadapters.model.CapabilityStatus;
import com.sellerintelligence.modules.marketplaceadapters.model.ExecutionContext;
import com.sellerintelligence.modules.marketplaceadapters.model.MarketplaceCapability;
import com.sellerintelligence.modules.marketplaceadapters.model.MarketplaceOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Orquestrador da esteira de resiliência integrada para adaptadores de marketplaces.
 * Executa as verificações em ordem estrita de segurança e contenção:
 * 1. Kill Switch
 * 2. Validação de Capacidade e Evidência (Fail-Closed)
 * 3. Idempotência
 * 4. Circuit Breaker
 * 5. Rate Limiter
 * 6. Bulkhead
 * 7. Retry com Full Jitter & Respeito a Retry-After
 * 8. Timeout
 */
public class ResiliencePipeline {

    private static final Logger log = LoggerFactory.getLogger(ResiliencePipeline.class);

    private final String marketplaceId;
    private final CapabilityRegistry capabilityRegistry;
    private final KillSwitch killSwitch;
    private final RateLimiter rateLimiter;
    private final Bulkhead bulkhead;
    private final CircuitBreaker circuitBreaker;
    private final RetryPolicy retryPolicy;
    private final TimeoutExecutor timeoutExecutor;
    private final IdempotencyManager idempotencyManager;
    private final Duration operationTimeout;

    public ResiliencePipeline(
            String marketplaceId,
            CapabilityRegistry capabilityRegistry,
            KillSwitch killSwitch,
            RateLimiter rateLimiter,
            Bulkhead bulkhead,
            CircuitBreaker circuitBreaker,
            RetryPolicy retryPolicy,
            TimeoutExecutor timeoutExecutor,
            IdempotencyManager idempotencyManager,
            Duration operationTimeout
    ) {
        this.marketplaceId = marketplaceId;
        this.capabilityRegistry = capabilityRegistry;
        this.killSwitch = killSwitch;
        this.rateLimiter = rateLimiter;
        this.bulkhead = bulkhead;
        this.circuitBreaker = circuitBreaker;
        this.retryPolicy = retryPolicy;
        this.timeoutExecutor = timeoutExecutor;
        this.idempotencyManager = idempotencyManager;
        this.operationTimeout = operationTimeout != null ? operationTimeout : Duration.ofSeconds(5);
    }

    /**
     * Executa a operação submetendo-a a todas as salvaguardas de resiliência e políticas de canal.
     */
    public <T, R> R execute(MarketplaceOperation<T, R> operation, T input, ExecutionContext context) throws Exception {
        String operationName = operation.getOperationName();
        MarketplaceCapability capability = operation.getRequiredCapability();

        log.debug("[{}] Iniciando execução da operação '{}' (Capacidade: {}) para tenant={}",
                marketplaceId, operationName, capability, context.tenantId());

        // 1. Verificação de Kill Switch
        if (killSwitch.isKilled(marketplaceId)) {
            throw new KillSwitchActivatedException(marketplaceId);
        }

        // 2. Verificação de Capacidade e Evidência Documental (Fail-Closed)
        verifyCapability(capability);

        // 3. Verificação de Idempotência
        String idempotencyKey = buildIdempotencyKey(operation, input, context);
        if (idempotencyKey != null && idempotencyManager.isProcessed(idempotencyKey)) {
            R cached = idempotencyManager.getCachedResult(idempotencyKey);
            log.info("[{}] Retornando resultado em cache por chave de idempotência '{}'", marketplaceId, idempotencyKey);
            return cached;
        }

        // 4. Verificação de Permissão no Circuit Breaker
        circuitBreaker.checkPermission();

        // 5. Rate Limiting
        rateLimiter.acquire();

        // 6. Bulkhead (Isolamento de Concorrência)
        bulkhead.acquire();

        try {
            // 7. Retry Policy envolvendo Timeout e Execução
            R result = retryPolicy.execute(() -> {
                return timeoutExecutor.executeWithTimeout(
                        () -> operation.execute(input, context),
                        operationTimeout,
                        marketplaceId
                );
            }, marketplaceId);

            // Sucesso operacional
            circuitBreaker.recordSuccess();

            if (idempotencyKey != null) {
                idempotencyManager.record(idempotencyKey, result);
            }

            log.debug("[{}] Operação '{}' concluída com sucesso.", marketplaceId, operationName);
            return result;

        } catch (Throwable ex) {
            circuitBreaker.recordFailure(ex);
            log.warn("[{}] Falha na operação '{}': {}", marketplaceId, operationName, AdapterLogSanitizer.mask(ex.getMessage()));
            if (ex instanceof Exception e) {
                throw e;
            }
            throw new RuntimeException(ex);
        } finally {
            bulkhead.release();
        }
    }

    private void verifyCapability(MarketplaceCapability capability) {
        CapabilityStatus status = capabilityRegistry.getStatus(capability);

        if (status == CapabilityStatus.UNVERIFIED) {
            throw new UnverifiedCapabilityException(marketplaceId, capability);
        }
        if (status == CapabilityStatus.UNSUPPORTED) {
            throw new UnsupportedCapabilityException(marketplaceId, capability);
        }
        if (status == CapabilityStatus.RESTRICTED) {
            throw new MarketplaceAdapterException(marketplaceId,
                    String.format("A capacidade '%s' está em estado RESTRICTED e desabilitada na fase MVP.", capability));
        }

        if (!capabilityRegistry.isExecutable(capability)) {
            throw new UnverifiedCapabilityException(marketplaceId, capability);
        }
    }

    private <T, R> String buildIdempotencyKey(MarketplaceOperation<T, R> operation, T input, ExecutionContext context) {
        String key = operation.getIdempotencyKey(input);
        if (key == null || key.isBlank()) {
            return null;
        }
        return String.format("%s:%s:%s:%s", context.tenantId(), marketplaceId, operation.getOperationName(), key);
    }
}
