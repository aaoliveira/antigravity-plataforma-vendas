package com.sellerintelligence.modules.marketplaceadapters.fake;

import com.sellerintelligence.modules.marketplaceadapters.api.AbstractMarketplaceAdapter;
import com.sellerintelligence.modules.marketplaceadapters.model.CapabilityEvidence;
import com.sellerintelligence.modules.marketplaceadapters.model.CapabilityRegistry;
import com.sellerintelligence.modules.marketplaceadapters.model.CapabilityStatus;
import com.sellerintelligence.modules.marketplaceadapters.model.MarketplaceCapability;
import com.sellerintelligence.modules.marketplaceadapters.resilience.Bulkhead;
import com.sellerintelligence.modules.marketplaceadapters.resilience.CircuitBreaker;
import com.sellerintelligence.modules.marketplaceadapters.resilience.IdempotencyManager;
import com.sellerintelligence.modules.marketplaceadapters.resilience.KillSwitch;
import com.sellerintelligence.modules.marketplaceadapters.resilience.RateLimiter;
import com.sellerintelligence.modules.marketplaceadapters.resilience.ResiliencePipeline;
import com.sellerintelligence.modules.marketplaceadapters.resilience.RetryPolicy;
import com.sellerintelligence.modules.marketplaceadapters.resilience.TimeoutExecutor;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Adaptador Fake configurável para testes unitários e de integração.
 * Permite simular rate limits, HTTP 429 com Retry-After, circuit breaking, timeouts
 * e falhas em rede sem realizar requisições externas reais.
 */
public class FakeMarketplaceAdapter extends AbstractMarketplaceAdapter {

    public static final String FAKE_MARKETPLACE_ID = "MOCK_MARKETPLACE";

    private final KillSwitch killSwitch;
    private final RateLimiter rateLimiter;
    private final Bulkhead bulkhead;
    private final CircuitBreaker circuitBreaker;
    private final RetryPolicy retryPolicy;
    private final IdempotencyManager idempotencyManager;

    private final AtomicInteger invocationCount = new AtomicInteger(0);

    public FakeMarketplaceAdapter(
            String marketplaceId,
            CapabilityRegistry capabilityRegistry,
            KillSwitch killSwitch,
            RateLimiter rateLimiter,
            Bulkhead bulkhead,
            CircuitBreaker circuitBreaker,
            RetryPolicy retryPolicy,
            TimeoutExecutor timeoutExecutor,
            IdempotencyManager idempotencyManager,
            Duration timeout
    ) {
        super(
                marketplaceId,
                capabilityRegistry,
                new ResiliencePipeline(
                        marketplaceId,
                        capabilityRegistry,
                        killSwitch,
                        rateLimiter,
                        bulkhead,
                        circuitBreaker,
                        retryPolicy,
                        timeoutExecutor,
                        idempotencyManager,
                        timeout
                )
        );
        this.killSwitch = killSwitch;
        this.rateLimiter = rateLimiter;
        this.bulkhead = bulkhead;
        this.circuitBreaker = circuitBreaker;
        this.retryPolicy = retryPolicy;
        this.idempotencyManager = idempotencyManager;
    }

    /**
     * Cria uma instância de teste pré-configurada com parâmetros ágeis para testes rápidos.
     */
    public static FakeMarketplaceAdapter createDefault() {
        String mktId = FAKE_MARKETPLACE_ID;
        CapabilityRegistry registry = new CapabilityRegistry(mktId);

        // Registra READ_OWN_CATALOG como SUPPORTED com evidência oficial válida
        registry.register(
                MarketplaceCapability.READ_OWN_CATALOG,
                CapabilityStatus.SUPPORTED,
                new CapabilityEvidence(
                        "https://developers.example.com/api/catalog",
                        LocalDate.of(2026, 11, 1),
                        List.of("read", "catalog_read"),
                        "20 req/s por token",
                        "Apenas leitura no MVP",
                        "v2.1",
                        "Staff Engineer QA"
                )
        );

        // Registra READ_OWN_ORDERS como SUPPORTED com evidência
        registry.register(
                MarketplaceCapability.READ_OWN_ORDERS,
                CapabilityStatus.SUPPORTED,
                new CapabilityEvidence(
                        "https://developers.example.com/api/orders",
                        LocalDate.of(2026, 11, 1),
                        List.of("read", "orders_read"),
                        "20 req/s por token",
                        "Apenas leitura no MVP",
                        "v2.1",
                        "Staff Engineer QA"
                )
        );

        // Registra COMPETITOR_SALES_VOLUME como UNSUPPORTED
        registry.register(
                MarketplaceCapability.COMPETITOR_SALES_VOLUME,
                CapabilityStatus.UNSUPPORTED,
                null
        );

        // Registra WRITE_OPERATIONS como RESTRICTED
        registry.register(
                MarketplaceCapability.WRITE_OPERATIONS,
                CapabilityStatus.RESTRICTED,
                null
        );

        // Registra MARKET_SEARCH como UNVERIFIED (sem evidência)
        registry.register(
                MarketplaceCapability.MARKET_SEARCH,
                CapabilityStatus.UNVERIFIED,
                null
        );

        KillSwitch killSwitch = new KillSwitch();
        RateLimiter rateLimiter = new RateLimiter(mktId, 50.0);
        Bulkhead bulkhead = new Bulkhead(mktId, 5);
        CircuitBreaker circuitBreaker = new CircuitBreaker(mktId, 3, Duration.ofMillis(200));
        RetryPolicy retryPolicy = new RetryPolicy(3, Duration.ofMillis(10), Duration.ofMillis(50));
        TimeoutExecutor timeoutExecutor = new TimeoutExecutor();
        IdempotencyManager idempotencyManager = new IdempotencyManager(60_000L);

        return new FakeMarketplaceAdapter(
                mktId,
                registry,
                killSwitch,
                rateLimiter,
                bulkhead,
                circuitBreaker,
                retryPolicy,
                timeoutExecutor,
                idempotencyManager,
                Duration.ofMillis(500)
        );
    }

    public KillSwitch getKillSwitch() {
        return killSwitch;
    }

    public RateLimiter getRateLimiter() {
        return rateLimiter;
    }

    public Bulkhead getBulkhead() {
        return bulkhead;
    }

    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }

    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    public IdempotencyManager getIdempotencyManager() {
        return idempotencyManager;
    }

    public int getInvocationCount() {
        return invocationCount.get();
    }

    public void incrementInvocation() {
        invocationCount.incrementAndGet();
    }
}
