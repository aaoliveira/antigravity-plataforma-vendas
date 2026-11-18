package com.sellerintelligence.modules.marketplaceadapters.shopee;

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

/**
 * Adaptador oficial da Shopee Open Platform v2 integrado à esteira de resiliência e políticas do PRD.
 * Se não houver acesso aprovado de parceiro ou credenciais de produção válidas,
 * mantém todas as capacidades como UNVERIFIED e bloqueia chamadas reais (Fail-Closed).
 */
public class ShopeeMarketplaceAdapter extends AbstractMarketplaceAdapter {

    public static final String MARKETPLACE_ID = "SHOPEE";

    private final boolean approvedPartnerAccess;

    public ShopeeMarketplaceAdapter(
            boolean approvedPartnerAccess,
            KillSwitch killSwitch,
            RateLimiter rateLimiter,
            Bulkhead bulkhead,
            CircuitBreaker circuitBreaker,
            RetryPolicy retryPolicy,
            TimeoutExecutor timeoutExecutor,
            IdempotencyManager idempotencyManager
    ) {
        this(
                approvedPartnerAccess,
                buildRegistry(approvedPartnerAccess),
                killSwitch,
                rateLimiter,
                bulkhead,
                circuitBreaker,
                retryPolicy,
                timeoutExecutor,
                idempotencyManager
        );
    }

    private ShopeeMarketplaceAdapter(
            boolean approvedPartnerAccess,
            CapabilityRegistry registry,
            KillSwitch killSwitch,
            RateLimiter rateLimiter,
            Bulkhead bulkhead,
            CircuitBreaker circuitBreaker,
            RetryPolicy retryPolicy,
            TimeoutExecutor timeoutExecutor,
            IdempotencyManager idempotencyManager
    ) {
        super(
                MARKETPLACE_ID,
                registry,
                new ResiliencePipeline(
                        MARKETPLACE_ID,
                        registry,
                        killSwitch,
                        rateLimiter,
                        bulkhead,
                        circuitBreaker,
                        retryPolicy,
                        timeoutExecutor,
                        idempotencyManager,
                        Duration.ofSeconds(5)
                )
        );
        this.approvedPartnerAccess = approvedPartnerAccess;
    }

    public boolean hasApprovedPartnerAccess() {
        return approvedPartnerAccess;
    }

    private static CapabilityRegistry buildRegistry(boolean approvedPartnerAccess) {
        CapabilityRegistry registry = new CapabilityRegistry(MARKETPLACE_ID);

        if (!approvedPartnerAccess) {
            // Regra Fail-Closed: sem acesso aprovado à plataforma ou documentação suficiente,
            // mantém adapter UNVERIFIED e bloqueia chamadas reais.
            for (MarketplaceCapability cap : MarketplaceCapability.values()) {
                registry.register(cap, CapabilityStatus.UNVERIFIED, null);
            }
            return registry;
        }

        // Com acesso aprovado / em modo sandbox/test autorizado:
        registry.register(
                MarketplaceCapability.OFFICIAL_AUTH,
                CapabilityStatus.SUPPORTED,
                new CapabilityEvidence(
                        "https://open.shopee.com",
                        LocalDate.of(2026, 11, 18),
                        List.of("Shop Level (OpenAPI v2)"),
                        "5-10 req/s por loja (HMAC-SHA256)",
                        "Assinatura criptográfica por requisição",
                        "OpenAPI v2.0",
                        "Tech Lead / Staff SE"
                )
        );

        registry.register(
                MarketplaceCapability.READ_OWN_CATALOG,
                CapabilityStatus.SUPPORTED,
                new CapabilityEvidence(
                        "https://open.shopee.com/documents/v2/v2.product.get_item_list",
                        LocalDate.of(2026, 11, 18),
                        List.of("Shop Level"),
                        "10 req/s",
                        "Apenas itens da própria loja do lojista",
                        "OpenAPI v2.0",
                        "Tech Lead / Staff SE"
                )
        );

        registry.register(
                MarketplaceCapability.READ_OWN_ORDERS,
                CapabilityStatus.SUPPORTED,
                new CapabilityEvidence(
                        "https://open.shopee.com/documents/v2/v2.order.get_order_list",
                        LocalDate.of(2026, 11, 18),
                        List.of("Shop Level"),
                        "10 req/s",
                        "Pedidos e movimentação do lojista",
                        "OpenAPI v2.0",
                        "Tech Lead / Staff SE"
                )
        );

        registry.register(
                MarketplaceCapability.MARKET_SEARCH,
                CapabilityStatus.RESTRICTED,
                new CapabilityEvidence(
                        "https://open.shopee.com/documents/v2/guidelines",
                        LocalDate.of(2026, 11, 18),
                        List.of("Shop Level"),
                        "Quotas restritas",
                        "Permitido apenas dentro de limites oficiais da plataforma",
                        "OpenAPI v2.0",
                        "Tech Lead / Staff SE"
                )
        );

        registry.register(
                MarketplaceCapability.OBSERVED_PRICE_SNAPSHOT,
                CapabilityStatus.RESTRICTED,
                new CapabilityEvidence(
                        "https://open.shopee.com/documents/v2/guidelines",
                        LocalDate.of(2026, 11, 18),
                        List.of("Shop Level"),
                        "Itens previamente cadastrados para monitoramento",
                        "Armazenamento progressivo no banco da aplicação",
                        "OpenAPI v2.0",
                        "Tech Lead / Staff SE"
                )
        );

        registry.register(
                MarketplaceCapability.OFFICIAL_LISTING_METRICS,
                CapabilityStatus.SUPPORTED,
                new CapabilityEvidence(
                        "https://open.shopee.com/documents/v2/v2.analytics",
                        LocalDate.of(2026, 11, 18),
                        List.of("Shop Level"),
                        "10 req/s",
                        "Métricas oficiais concedidas pelo canal",
                        "OpenAPI v2.0",
                        "Tech Lead / Staff SE"
                )
        );

        registry.register(
                MarketplaceCapability.COMPETITOR_SALES_VOLUME,
                CapabilityStatus.UNSUPPORTED,
                new CapabilityEvidence(
                        "https://open.shopee.com/policies",
                        LocalDate.of(2026, 11, 18),
                        List.of(),
                        "Zero requisições",
                        "A Shopee não fornece volume de vendas de terceiros. Scraping expressamente proibido.",
                        "OpenAPI v2.0",
                        "Tech Lead / Staff SE"
                )
        );

        registry.register(
                MarketplaceCapability.WRITE_OPERATIONS,
                CapabilityStatus.RESTRICTED,
                new CapabilityEvidence(
                        "https://sellerintelligence.com.br/governance/mvp-security",
                        LocalDate.of(2026, 11, 18),
                        List.of("Somente Leitura"),
                        "Bloqueio operacional",
                        "Desabilitado no MVP para garantia de risco zero ao catálogo do seller.",
                        "OpenAPI v2.0",
                        "Tech Lead / Staff SE"
                )
        );

        return registry;
    }
}
