package com.sellerintelligence.modules.marketplaceadapters.shopee;

import com.sellerintelligence.modules.marketplaceadapters.exception.KillSwitchActivatedException;
import com.sellerintelligence.modules.marketplaceadapters.exception.UnsupportedCapabilityException;
import com.sellerintelligence.modules.marketplaceadapters.exception.UnverifiedCapabilityException;
import com.sellerintelligence.modules.marketplaceadapters.model.CapabilityStatus;
import com.sellerintelligence.modules.marketplaceadapters.model.ExecutionContext;
import com.sellerintelligence.modules.marketplaceadapters.model.MarketplaceCapability;
import com.sellerintelligence.modules.marketplaceadapters.model.MarketplaceOperation;
import com.sellerintelligence.modules.marketplaceadapters.resilience.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShopeeMarketplaceAdapterTest {

    private KillSwitch killSwitch;
    private RateLimiter rateLimiter;
    private Bulkhead bulkhead;
    private CircuitBreaker circuitBreaker;
    private RetryPolicy retryPolicy;
    private TimeoutExecutor timeoutExecutor;
    private IdempotencyManager idempotencyManager;

    private ExecutionContext context;

    @BeforeEach
    void setUp() {
        killSwitch = new KillSwitch();
        rateLimiter = new RateLimiter("SHOPEE", 10.0);
        bulkhead = new Bulkhead("SHOPEE", 5);
        circuitBreaker = new CircuitBreaker("SHOPEE", 5, Duration.ofSeconds(10));
        retryPolicy = new RetryPolicy(3, Duration.ofMillis(10), Duration.ofMillis(50));
        timeoutExecutor = new TimeoutExecutor();
        idempotencyManager = new IdempotencyManager();

        context = new ExecutionContext(
                UUID.randomUUID(),
                "test-seller-account",
                "enc-access-token",
                "corr-shopee-test"
        );
    }

    @Test
    @DisplayName("1. Na ausência de acesso de parceiro aprovado, adapter permanece UNVERIFIED e bloqueia chamadas reais (Fail-Closed)")
    void testUnverifiedWhenNoApprovedAccess() {
        ShopeeMarketplaceAdapter unverifiedAdapter = new ShopeeMarketplaceAdapter(
                false, // Sem acesso aprovado
                killSwitch,
                rateLimiter,
                bulkhead,
                circuitBreaker,
                retryPolicy,
                timeoutExecutor,
                idempotencyManager
        );

        assertThat(unverifiedAdapter.hasApprovedPartnerAccess()).isFalse();
        assertThat(unverifiedAdapter.getCapabilityRegistry().getStatus(MarketplaceCapability.READ_OWN_CATALOG))
                .isEqualTo(CapabilityStatus.UNVERIFIED);

        MarketplaceOperation<String, String> testOperation = new MarketplaceOperation<>() {
            @Override
            public String getOperationName() {
                return "GET_CATALOG";
            }

            @Override
            public MarketplaceCapability getRequiredCapability() {
                return MarketplaceCapability.READ_OWN_CATALOG;
            }

            @Override
            public String execute(String input, ExecutionContext ctx) {
                return "catalog_items";
            }
        };

        assertThatThrownBy(() -> unverifiedAdapter.execute(testOperation, "payload", context))
                .isInstanceOf(UnverifiedCapabilityException.class)
                .hasMessageContaining("UNVERIFIED");
    }

    @Test
    @DisplayName("2. Com acesso aprovado (sandbox/test), capacidades homologadas são SUPPORTED e executam com sucesso")
    void testSupportedWhenApprovedAccess() throws Exception {
        ShopeeMarketplaceAdapter adapter = new ShopeeMarketplaceAdapter(
                true, // Acesso de teste homologado
                killSwitch,
                rateLimiter,
                bulkhead,
                circuitBreaker,
                retryPolicy,
                timeoutExecutor,
                idempotencyManager
        );

        assertThat(adapter.hasApprovedPartnerAccess()).isTrue();
        assertThat(adapter.getCapabilityRegistry().getStatus(MarketplaceCapability.OFFICIAL_AUTH))
                .isEqualTo(CapabilityStatus.SUPPORTED);
        assertThat(adapter.getCapabilityRegistry().getStatus(MarketplaceCapability.READ_OWN_CATALOG))
                .isEqualTo(CapabilityStatus.SUPPORTED);
        assertThat(adapter.getCapabilityRegistry().getStatus(MarketplaceCapability.READ_OWN_ORDERS))
                .isEqualTo(CapabilityStatus.SUPPORTED);
        assertThat(adapter.getCapabilityRegistry().getStatus(MarketplaceCapability.COMPETITOR_SALES_VOLUME))
                .isEqualTo(CapabilityStatus.UNSUPPORTED);

        MarketplaceOperation<String, String> testOperation = new MarketplaceOperation<>() {
            @Override
            public String getOperationName() {
                return "GET_ORDERS";
            }

            @Override
            public MarketplaceCapability getRequiredCapability() {
                return MarketplaceCapability.READ_OWN_ORDERS;
            }

            @Override
            public String execute(String input, ExecutionContext ctx) {
                return "order_list_success";
            }
        };

        String result = adapter.execute(testOperation, "query", context);
        assertThat(result).isEqualTo("order_list_success");
    }

    @Test
    @DisplayName("3. Rejeita terminantemente operações em capacidades UNSUPPORTED (volume de vendas de terceiros)")
    void testUnsupportedCapabilityRejected() {
        ShopeeMarketplaceAdapter adapter = new ShopeeMarketplaceAdapter(
                true,
                killSwitch,
                rateLimiter,
                bulkhead,
                circuitBreaker,
                retryPolicy,
                timeoutExecutor,
                idempotencyManager
        );

        MarketplaceOperation<String, String> competitorOperation = new MarketplaceOperation<>() {
            @Override
            public String getOperationName() {
                return "SCRAPE_COMPETITOR_SALES";
            }

            @Override
            public MarketplaceCapability getRequiredCapability() {
                return MarketplaceCapability.COMPETITOR_SALES_VOLUME;
            }

            @Override
            public String execute(String input, ExecutionContext ctx) {
                return "forged_data";
            }
        };

        assertThatThrownBy(() -> adapter.execute(competitorOperation, "competitor-shop-id", context))
                .isInstanceOf(UnsupportedCapabilityException.class)
                .hasMessageContaining("UNSUPPORTED");
    }

    @Test
    @DisplayName("4. Kill Switch bloqueia imediatamente execuções no conector da Shopee")
    void testKillSwitchBlocksExecution() {
        ShopeeMarketplaceAdapter adapter = new ShopeeMarketplaceAdapter(
                true,
                killSwitch,
                rateLimiter,
                bulkhead,
                circuitBreaker,
                retryPolicy,
                timeoutExecutor,
                idempotencyManager
        );

        killSwitch.activate("SHOPEE");

        MarketplaceOperation<String, String> validOp = new MarketplaceOperation<>() {
            @Override
            public String getOperationName() {
                return "HEALTH_CHECK";
            }

            @Override
            public MarketplaceCapability getRequiredCapability() {
                return MarketplaceCapability.READ_OWN_CATALOG;
            }

            @Override
            public String execute(String input, ExecutionContext ctx) {
                return "ok";
            }
        };

        assertThatThrownBy(() -> adapter.execute(validOp, "input", context))
                .isInstanceOf(KillSwitchActivatedException.class);
    }
}
