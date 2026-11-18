package com.sellerintelligence.modules.marketplaceadapters;

import com.sellerintelligence.modules.marketplaceadapters.exception.BulkheadFullException;
import com.sellerintelligence.modules.marketplaceadapters.exception.CircuitBreakerOpenException;
import com.sellerintelligence.modules.marketplaceadapters.exception.KillSwitchActivatedException;
import com.sellerintelligence.modules.marketplaceadapters.exception.RateLimitExceededException;
import com.sellerintelligence.modules.marketplaceadapters.exception.RetryExhaustedException;
import com.sellerintelligence.modules.marketplaceadapters.exception.UnsupportedCapabilityException;
import com.sellerintelligence.modules.marketplaceadapters.exception.UnverifiedCapabilityException;
import com.sellerintelligence.modules.marketplaceadapters.fake.FakeMarketplaceAdapter;
import com.sellerintelligence.modules.marketplaceadapters.model.ExecutionContext;
import com.sellerintelligence.modules.marketplaceadapters.model.MarketplaceCapability;
import com.sellerintelligence.modules.marketplaceadapters.model.MarketplaceOperation;
import com.sellerintelligence.modules.marketplaceadapters.resilience.AdapterLogSanitizer;
import com.sellerintelligence.modules.marketplaceadapters.resilience.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarketplaceAdapterResilienceTest {

    private FakeMarketplaceAdapter adapter;
    private ExecutionContext context;

    @BeforeEach
    void setUp() {
        adapter = FakeMarketplaceAdapter.createDefault();
        context = ExecutionContext.of(
                UUID.randomUUID(),
                "SELLER_ACC_123",
                "encrypted_token_value"
        );
    }

    @Test
    @DisplayName("Cenário 1: Operação com capacidade UNVERIFIED é bloqueada sem executar chamada")
    void testUnverifiedCapabilityBlockedWithoutExecution() {
        MarketplaceOperation<String, String> unverifiedOp = new MarketplaceOperation<>() {
            @Override
            public MarketplaceCapability getRequiredCapability() {
                return MarketplaceCapability.MARKET_SEARCH; // Registrado como UNVERIFIED
            }

            @Override
            public String getOperationName() {
                return "SEARCH_MARKET";
            }

            @Override
            public String execute(String input, ExecutionContext ctx) {
                adapter.incrementInvocation();
                return "result";
            }
        };

        assertThatThrownBy(() -> adapter.execute(unverifiedOp, "query", context))
                .isInstanceOf(UnverifiedCapabilityException.class)
                .hasMessageContaining("UNVERIFIED");

        assertThat(adapter.getInvocationCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Cenário 2: Operação com capacidade UNSUPPORTED é categoricamente bloqueada (Anti-Scraping)")
    void testUnsupportedCapabilityBlockedWithoutExecution() {
        MarketplaceOperation<String, String> unsupportedOp = new MarketplaceOperation<>() {
            @Override
            public MarketplaceCapability getRequiredCapability() {
                return MarketplaceCapability.COMPETITOR_SALES_VOLUME; // Registrado como UNSUPPORTED
            }

            @Override
            public String getOperationName() {
                return "GET_COMPETITOR_SALES";
            }

            @Override
            public String execute(String input, ExecutionContext ctx) {
                adapter.incrementInvocation();
                return "forbidden_data";
            }
        };

        assertThatThrownBy(() -> adapter.execute(unsupportedOp, "competitor_id", context))
                .isInstanceOf(UnsupportedCapabilityException.class)
                .hasMessageContaining("UNSUPPORTED");

        assertThat(adapter.getInvocationCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Cenário 3: Kill Switch ativado bloqueia qualquer chamada imediatamente")
    void testKillSwitchBlocksCallsImmediately() throws Exception {
        MarketplaceOperation<String, String> validOp = createValidCatalogOperation();

        // 1. Chamada legítima antes do Kill Switch funciona
        String initialResult = adapter.execute(validOp, "input", context);
        assertThat(initialResult).isEqualTo("catalog_data");
        assertThat(adapter.getInvocationCount()).isEqualTo(1);

        // 2. Ativa o Kill Switch para o canal
        adapter.getKillSwitch().activate(adapter.getMarketplaceId());

        // 3. Próxima chamada é imediatamente barrada
        assertThatThrownBy(() -> adapter.execute(validOp, "input", context))
                .isInstanceOf(KillSwitchActivatedException.class)
                .hasMessageContaining("Kill Switch");

        // Nenhuma nova invocação ocorreu
        assertThat(adapter.getInvocationCount()).isEqualTo(1);

        // 4. Desativa o Kill Switch e verifica recuperação
        adapter.getKillSwitch().deactivate(adapter.getMarketplaceId());
        String recoveredResult = adapter.execute(validOp, "input", context);
        assertThat(recoveredResult).isEqualTo("catalog_data");
        assertThat(adapter.getInvocationCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Cenário 4: Rate Limiter bloqueia quando quota de requisições por segundo é excedida")
    void testRateLimiterEnforcesQuota() {
        // Cria adaptador com quota severa de 1.0 token
        FakeMarketplaceAdapter strictAdapter = new FakeMarketplaceAdapter(
                "STRICT_MKT",
                adapter.getCapabilityRegistry(),
                adapter.getKillSwitch(),
                new com.sellerintelligence.modules.marketplaceadapters.resilience.RateLimiter("STRICT_MKT", 1.0),
                adapter.getBulkhead(),
                adapter.getCircuitBreaker(),
                adapter.getRetryPolicy(),
                new com.sellerintelligence.modules.marketplaceadapters.resilience.TimeoutExecutor(),
                adapter.getIdempotencyManager(),
                Duration.ofSeconds(1)
        );

        MarketplaceOperation<String, String> op = createValidCatalogOperation();

        // 1ª chamada consome o token disponível
        assertThat(strictAdapter.getRateLimiter().tryAcquire()).isTrue();

        // 2ª chamada imediata estoura a cota e lança RateLimitExceededException
        assertThatThrownBy(() -> strictAdapter.getRateLimiter().acquire())
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("Taxa de requisições excedida");
    }

    @Test
    @DisplayName("Cenário 5: Retry Policy não excede o limite configurado de tentativas")
    void testRetryPolicyDoesNotExceedMaxAttempts() {
        AtomicInteger attemptsCounter = new AtomicInteger(0);

        MarketplaceOperation<String, String> failingOp = new MarketplaceOperation<>() {
            @Override
            public MarketplaceCapability getRequiredCapability() {
                return MarketplaceCapability.READ_OWN_CATALOG;
            }

            @Override
            public String getOperationName() {
                return "FAILING_SYNC";
            }

            @Override
            public String execute(String input, ExecutionContext ctx) throws Exception {
                attemptsCounter.incrementAndGet();
                throw new IOException("Instabilidade de rede transitória");
            }
        };

        assertThatThrownBy(() -> adapter.execute(failingOp, "input", context))
                .isInstanceOf(RetryExhaustedException.class)
                .hasMessageContaining("Número máximo de tentativas esgotado");

        // maxAttempts = 3
        assertThat(attemptsCounter.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("Cenário 6: Retry Policy respeita tempo indicado por Retry-After")
    void testRetryPolicyRespectsRetryAfter() throws Exception {
        AtomicInteger attempts = new AtomicInteger(0);

        MarketplaceOperation<String, String> opWithRetryAfter = new MarketplaceOperation<>() {
            @Override
            public MarketplaceCapability getRequiredCapability() {
                return MarketplaceCapability.READ_OWN_CATALOG;
            }

            @Override
            public String getOperationName() {
                return "RETRY_AFTER_OP";
            }

            @Override
            public String execute(String input, ExecutionContext ctx) {
                int count = attempts.incrementAndGet();
                if (count == 1) {
                    // 1ª tentativa retorna HTTP 429 com 30ms de espera
                    throw new RateLimitExceededException(
                            adapter.getMarketplaceId(),
                            "Rate limit atingido pelo canal",
                            Duration.ofMillis(30)
                    );
                }
                adapter.incrementInvocation();
                return "success_after_wait";
            }
        };

        long startTime = System.currentTimeMillis();
        String result = adapter.execute(opWithRetryAfter, "input", context);
        long elapsed = System.currentTimeMillis() - startTime;

        assertThat(result).isEqualTo("success_after_wait");
        assertThat(attempts.get()).isEqualTo(2);
        // Garante que houve espera respeitando o Retry-After
        assertThat(elapsed).isGreaterThanOrEqualTo(25L);
    }

    @Test
    @DisplayName("Cenário 7: Circuit Breaker abre após falhas consecutivas e recupera após timeout")
    void testCircuitBreakerOpensAndRecovers() throws Exception {
        CircuitBreaker cb = adapter.getCircuitBreaker();
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        MarketplaceOperation<String, String> failingOp = new MarketplaceOperation<>() {
            @Override
            public MarketplaceCapability getRequiredCapability() {
                return MarketplaceCapability.READ_OWN_CATALOG;
            }

            @Override
            public String getOperationName() {
                return "NETWORK_FAIL";
            }

            @Override
            public String execute(String input, ExecutionContext ctx) throws Exception {
                throw new IOException("Falha no servidor parceiro");
            }
        };

        // Executa 3 chamadas com falha para atingir o limiar de 3 erros
        for (int i = 0; i < 3; i++) {
            try {
                adapter.execute(failingOp, "input", context);
            } catch (Exception ignored) {
            }
        }

        // Circuit breaker deve estar ABERTO (OPEN)
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // 4ª chamada deve falhar IMEDIATAMENTE com CircuitBreakerOpenException sem tocar no adaptador
        int countBefore = adapter.getInvocationCount();
        assertThatThrownBy(() -> adapter.execute(failingOp, "input", context))
                .isInstanceOf(CircuitBreakerOpenException.class);
        assertThat(adapter.getInvocationCount()).isEqualTo(countBefore);

        // Aguarda expiração do resetTimeout (200 ms)
        Thread.sleep(220);

        // Chamada agora deve entrar em HALF_OPEN e recuperar o circuito se for bem-sucedida
        MarketplaceOperation<String, String> validOp = createValidCatalogOperation();
        String recoveryResult = adapter.execute(validOp, "input", context);

        assertThat(recoveryResult).isEqualTo("catalog_data");
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("Cenário 8: Bulkhead isola e rejeita chamadas excedentes além do limite de concorrência")
    void testBulkheadRejectsExcessiveConcurrentCalls() throws Exception {
        // Cria adaptador com concorrência máxima de 1 chamada simultânea
        FakeMarketplaceAdapter singleSlotAdapter = new FakeMarketplaceAdapter(
                "SINGLE_BULKHEAD",
                adapter.getCapabilityRegistry(),
                adapter.getKillSwitch(),
                adapter.getRateLimiter(),
                new com.sellerintelligence.modules.marketplaceadapters.resilience.Bulkhead("SINGLE_BULKHEAD", 1),
                adapter.getCircuitBreaker(),
                adapter.getRetryPolicy(),
                new com.sellerintelligence.modules.marketplaceadapters.resilience.TimeoutExecutor(),
                adapter.getIdempotencyManager(),
                Duration.ofSeconds(2)
        );

        CountDownLatch task1Started = new CountDownLatch(1);
        CountDownLatch task1CanFinish = new CountDownLatch(1);

        MarketplaceOperation<String, String> slowOp = new MarketplaceOperation<>() {
            @Override
            public MarketplaceCapability getRequiredCapability() {
                return MarketplaceCapability.READ_OWN_CATALOG;
            }

            @Override
            public String getOperationName() {
                return "SLOW_OP";
            }

            @Override
            public String execute(String input, ExecutionContext ctx) throws Exception {
                task1Started.countDown();
                task1CanFinish.await(2, TimeUnit.SECONDS);
                return "slow_done";
            }
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<String> future1 = executor.submit(() -> singleSlotAdapter.execute(slowOp, "input1", context));

            // Aguarda a Task 1 ocupar o único slot do Bulkhead
            assertThat(task1Started.await(1, TimeUnit.SECONDS)).isTrue();

            // Task 2 tenta executar simultaneamente -> deve ser rejeitada com BulkheadFullException
            assertThatThrownBy(() -> singleSlotAdapter.execute(slowOp, "input2", context))
                    .isInstanceOf(BulkheadFullException.class)
                    .hasMessageContaining("Capacidade máxima do Bulkhead atingida");

            // Libera a Task 1
            task1CanFinish.countDown();
            assertThat(future1.get()).isEqualTo("slow_done");

        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("Cenário 9: Idempotência impede processamento duplicado de requisições idênticas")
    void testIdempotencyPreventsDuplicateExecution() throws Exception {
        MarketplaceOperation<String, String> idempotentOp = new MarketplaceOperation<>() {
            @Override
            public MarketplaceCapability getRequiredCapability() {
                return MarketplaceCapability.READ_OWN_CATALOG;
            }

            @Override
            public String getOperationName() {
                return "IDEMPOTENT_CATALOG_SYNC";
            }

            @Override
            public String execute(String input, ExecutionContext ctx) {
                adapter.incrementInvocation();
                return "synced_page_" + input;
            }

            @Override
            public String getIdempotencyKey(String input) {
                return "sync-key-" + input;
            }
        };

        // 1ª chamada executa a lógica real
        String result1 = adapter.execute(idempotentOp, "page_1", context);
        assertThat(result1).isEqualTo("synced_page_page_1");
        assertThat(adapter.getInvocationCount()).isEqualTo(1);

        // 2ª chamada com a mesma chave retorna resultado do cache sem reexecutar
        String result2 = adapter.execute(idempotentOp, "page_1", context);
        assertThat(result2).isEqualTo("synced_page_page_1");
        assertThat(adapter.getInvocationCount()).isEqualTo(1); // Continua 1!
    }

    @Test
    @DisplayName("Cenário 10: Logs do adaptador mascaram rigorosamente dados sensíveis e credenciais")
    void testLogSanitizerMasksSensitiveData() {
        String rawLog = "Payload: {\"access_token\": \"APP_USR-secret-123\", \"client_secret\": \"key_999\", \"password\": \"mySecretPass\"} and query: refresh_token=ref_xyz&other=public";
        String masked = AdapterLogSanitizer.mask(rawLog);

        assertThat(masked).doesNotContain("APP_USR-secret-123");
        assertThat(masked).doesNotContain("key_999");
        assertThat(masked).doesNotContain("mySecretPass");
        assertThat(masked).doesNotContain("ref_xyz");

        assertThat(masked).contains("\"access_token\": \"[REDACTED]\"");
        assertThat(masked).contains("\"client_secret\": \"[REDACTED]\"");
        assertThat(masked).contains("\"password\": \"[REDACTED]\"");
        assertThat(masked).contains("refresh_token=[REDACTED]");
        assertThat(masked).contains("other=public");
    }

    private MarketplaceOperation<String, String> createValidCatalogOperation() {
        return new MarketplaceOperation<>() {
            @Override
            public MarketplaceCapability getRequiredCapability() {
                return MarketplaceCapability.READ_OWN_CATALOG;
            }

            @Override
            public String getOperationName() {
                return "READ_CATALOG";
            }

            @Override
            public String execute(String input, ExecutionContext ctx) {
                adapter.incrementInvocation();
                return "catalog_data";
            }
        };
    }
}
