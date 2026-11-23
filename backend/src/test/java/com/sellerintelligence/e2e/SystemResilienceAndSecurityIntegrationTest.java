package com.sellerintelligence.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellerintelligence.core.security.BruteForceProtectionService;
import com.sellerintelligence.core.security.JwtTokenProvider;
import com.sellerintelligence.modules.audit.AuditEvent;
import com.sellerintelligence.modules.audit.AuditEventRepository;
import com.sellerintelligence.modules.catalog.SellerAccount;
import com.sellerintelligence.modules.catalog.SellerAccountRepository;
import com.sellerintelligence.modules.company.entity.CompanyAccountLinkRecord;
import com.sellerintelligence.modules.company.entity.CompanyRecord;
import com.sellerintelligence.modules.company.repository.CompanyAccountLinkRepository;
import com.sellerintelligence.modules.company.repository.CompanyRepository;
import com.sellerintelligence.modules.financialanalytics.repository.OrderFinancialFactRepository;
import com.sellerintelligence.modules.financialanalytics.service.OrderFinancialFactService;
import com.sellerintelligence.modules.identityaccess.dto.LoginRequest;
import com.sellerintelligence.modules.marketplaceadapters.exception.CircuitBreakerOpenException;
import com.sellerintelligence.modules.marketplaceadapters.exception.RateLimitExceededException;
import com.sellerintelligence.modules.marketplaceadapters.fake.FakeMarketplaceAdapter;
import com.sellerintelligence.modules.marketplaceadapters.resilience.CircuitBreaker;
import com.sellerintelligence.modules.marketplaceadapters.resilience.RateLimiter;
import com.sellerintelligence.modules.marketplaceconnection.MarketplaceConnection;
import com.sellerintelligence.modules.marketplaceconnection.MarketplaceConnectionRepository;
import com.sellerintelligence.modules.marketplaceconnection.mercadolivre.MeliOAuthClient;
import com.sellerintelligence.modules.marketplaceconnection.mercadolivre.MeliTokenResponse;
import com.sellerintelligence.modules.marketplaceconnection.mercadolivre.dto.OAuthCallbackRequest;
import com.sellerintelligence.modules.orderingestion.Order;
import com.sellerintelligence.modules.orderingestion.OrderDlqRecord;
import com.sellerintelligence.modules.orderingestion.OrderDlqRecordRepository;
import com.sellerintelligence.modules.orderingestion.OrderItem;
import com.sellerintelligence.modules.orderingestion.OrderItemRepository;
import com.sellerintelligence.modules.orderingestion.OrderRepository;
import com.sellerintelligence.modules.orderingestion.model.OrderPageResponse;
import com.sellerintelligence.modules.orderingestion.model.OrderStatus;
import com.sellerintelligence.modules.orderingestion.model.OrderSyncResult;
import com.sellerintelligence.modules.orderingestion.model.RawOrder;
import com.sellerintelligence.modules.orderingestion.model.RawOrderItem;
import com.sellerintelligence.modules.orderingestion.service.OrderSyncService;
import com.sellerintelligence.modules.reporting.model.ExportDataset;
import com.sellerintelligence.modules.reporting.model.ExportFileFormat;
import com.sellerintelligence.modules.reporting.model.ExportJobResponseDto;
import com.sellerintelligence.modules.reporting.model.ExportRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * P023-BE/QA: Suíte de Integração e Resiliência contra Falhas Críticas e Ameaças de Segurança.
 *
 * Valida os 8 cenários exigidos pela plataforma:
 * 1. Falhas de OAuth (Anti-CSRF State inválido/adulterado)
 * 2. API externa indisponível (503 / Timeout / Circuit Breaker fail-closed)
 * 3. Rate limiting (429 Too Many Requests & Token Bucket com Retry-After)
 * 4. Token expirado (Fail-closed 401 Unauthorized sem bypass)
 * 5. Retry e idempotência contábil (Sem duplicação de fatos ou distorção financeira)
 * 6. DLQ (Dead Letter Queue para descarte seguro de cargas tóxicas/corrompidas)
 * 7. Duplicidade de exportação (Idempotência por idempotencyKey sem I/O redundante)
 * 8. Acesso cross-tenant (Bloqueio estrito 403/404 e isolamento analítico de dados)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("P023-BE/QA: SystemResilienceAndSecurityIntegrationTest — Cenários de Caos e Resiliência")
public class SystemResilienceAndSecurityIntegrationTest {

    private static final UUID TENANT_ALPHA_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TENANT_BETA_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static final UUID USER_ALPHA_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID USER_BETA_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private BruteForceProtectionService bruteForceProtectionService;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private SellerAccountRepository sellerAccountRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyAccountLinkRepository companyAccountLinkRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderFinancialFactRepository orderFinancialFactRepository;

    @Autowired
    private OrderFinancialFactService orderFinancialFactService;

    @Autowired
    private OrderSyncService orderSyncService;

    @Autowired
    private OrderDlqRecordRepository dlqRecordRepository;

    @Autowired
    private MarketplaceConnectionRepository marketplaceConnectionRepository;

    @MockitoBean
    private MeliOAuthClient meliOAuthClient;

    private String tokenAlpha;
    private String tokenBeta;

    @BeforeEach
    void setUp() {
        bruteForceProtectionService.reset();

        tokenAlpha = jwtTokenProvider.generateToken(
                USER_ALPHA_ID, "seller@example.com", TENANT_ALPHA_ID, List.of("ROLE_OWNER")
        );
        tokenBeta = jwtTokenProvider.generateToken(
                USER_BETA_ID, "competitor@example.com", TENANT_BETA_ID, List.of("ROLE_OWNER")
        );

        when(meliOAuthClient.buildAuthorizationUrl(anyString(), anyString())).thenAnswer(invocation -> {
            String state = invocation.getArgument(0);
            String challenge = invocation.getArgument(1);
            return "https://auth.mercadolivre.com.br/authorization?response_type=code&client_id=test-app&state="
                    + state + "&code_challenge=" + challenge + "&code_challenge_method=S256";
        });

        when(meliOAuthClient.exchangeAuthorizationCode(anyString(), anyString(), anyString())).thenReturn(
                new MeliTokenResponse(
                        "APP_USR-sandbox-access-token-resilience",
                        "bearer",
                        21600L,
                        "read offline_access",
                        "555666777",
                        "TG-sandbox-refresh-token-resilience"
                )
        );
    }

    // =========================================================================================
    // CENÁRIO 1: Falha de OAuth (State Anti-CSRF Inválido ou Adulterado)
    // =========================================================================================
    @Test
    @DisplayName("Cenário 1: Callback OAuth com stateToken inválido/adulterado deve falhar com 400 Bad Request")
    void testOAuthFailureWithInvalidStateToken() throws Exception {
        OAuthCallbackRequest maliciousCallback = new OAuthCallbackRequest(
                "malicious-auth-code",
                "forged-anti-csrf-token",
                "http://localhost:3000/callback"
        );

        mockMvc.perform(post("/api/v1/integrations/mercadolivre/callback")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAlpha)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maliciousCallback)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("State Anti-CSRF Inválido")))
                .andExpect(jsonPath("$.status", is(400)));

        // Nenhuma conexão de marketplace foi persistida
        List<MarketplaceConnection> connections = marketplaceConnectionRepository.findAllByTenantId(TENANT_ALPHA_ID);
        assertThat(connections).isEmpty();
    }

    // =========================================================================================
    // CENÁRIO 2: API Externa Indisponível (503 / Timeout / Circuit Breaker Fail-Closed)
    // =========================================================================================
    @Test
    @DisplayName("Cenário 2: API externa indisponível abre o Circuit Breaker e bloqueia novas requisições em Fail-Closed")
    void testExternalApiUnavailableTriggersCircuitBreakerFailClosed() {
        // Circuit Breaker configurado com threshold de 3 falhas consecutivas e 30s de timeout
        CircuitBreaker circuitBreaker = new CircuitBreaker("MERCADO_LIVRE", 3, Duration.ofSeconds(30));

        // Circuito inicialmente fechado (permite operações)
        circuitBreaker.checkPermission();

        // 1ª e 2ª falhas na API externa
        circuitBreaker.recordFailure(new RuntimeException("Simulated HTTP 503 Service Unavailable"));
        circuitBreaker.recordFailure(new RuntimeException("Simulated Connection Timeout"));
        circuitBreaker.checkPermission(); // Ainda fechado

        // 3ª falha consecutiva: atinge o threshold
        circuitBreaker.recordFailure(new RuntimeException("Simulated HTTP 503 Service Unavailable"));

        // A partir deste momento, o circuito abre e rejeita fail-closed sem inventar dados
        assertThatThrownBy(circuitBreaker::checkPermission)
                .isInstanceOf(CircuitBreakerOpenException.class)
                .hasMessageContaining("ABERTO")
                .hasMessageContaining("MERCADO_LIVRE");
    }

    // =========================================================================================
    // CENÁRIO 3: Rate Limiting (HTTP 429 Too Many Requests & Token Bucket com Retry-After)
    // =========================================================================================
    @Test
    @DisplayName("Cenário 3a: Limitador de taxa (RateLimiter) bloqueia requisições excedentes com Retry-After")
    void testRateLimiterEnforcesQuotaWithRetryAfter() {
        // Quota estrita de 1.0 requisição por segundo
        RateLimiter rateLimiter = new RateLimiter("SHOPEE", 1.0);

        // 1ª requisição permitida imediatamente
        rateLimiter.acquire();

        // 2ª requisição imediata estoura a quota e deve lançar RateLimitExceededException com Retry-After positivo
        assertThatThrownBy(rateLimiter::acquire)
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("Taxa de requisições excedida no canal 'SHOPEE'")
                .satisfies(ex -> {
                    RateLimitExceededException rateEx = (RateLimitExceededException) ex;
                    assertThat(rateEx.getRetryAfter()).isNotNull();
                    assertThat(rateEx.getRetryAfter().toMillis()).isGreaterThan(0);
                });
    }

    @Test
    @DisplayName("Cenário 3b: Ataque de força bruta aciona bloqueio com HTTP 429 Too Many Requests e evento de auditoria")
    void testBruteForceLockoutTriggersHttp429() throws Exception {
        LoginRequest badRequest = new LoginRequest("seller@example.com", "SenhaIncorreta", null);

        // 5 tentativas consecutivas com falha
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badRequest)))
                    .andExpect(status().isUnauthorized());
        }

        // A 6ª tentativa deve retornar categoricamente HTTP 429
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.title", is("Limite de Tentativas Excedido")))
                .andExpect(jsonPath("$.status", is(429)));

        List<AuditEvent> blockedEvents = auditEventRepository.findByEventType("BRUTE_FORCE_BLOCKED");
        assertThat(blockedEvents).isNotEmpty();
    }

    // =========================================================================================
    // CENÁRIO 4: Token JWT Expirado (Fail-Closed Security)
    // =========================================================================================
    @Test
    @DisplayName("Cenário 4: Requisições com token JWT expirado devem retornar HTTP 401 Unauthorized")
    void testExpiredJwtTokenFailsImmediately() throws Exception {
        // Token gerado com expiração retroativa (-60 minutos)
        String expiredToken = jwtTokenProvider.generateToken(
                USER_ALPHA_ID, "seller@example.com", TENANT_ALPHA_ID, List.of("ROLE_OWNER"), -60
        );

        mockMvc.perform(get("/api/v1/tenancy/current")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.title", is("Autenticação Requerida ou Inválida")));

        mockMvc.perform(get("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================================
    // CENÁRIO 5: Retry e Idempotência Contábil
    // =========================================================================================
    @Test
    @DisplayName("Cenário 5: Reprocessamento de fatos contábeis do mesmo pedido é estritamente idempotente")
    void testAccountingFactReprocessingIsStrictlyIdempotent() {
        SellerAccount account = sellerAccountRepository.save(new SellerAccount(
                UUID.randomUUID(),
                TENANT_ALPHA_ID,
                "MERCADO_LIVRE",
                "SELLER_IDEMP_01",
                "Loja Idempotência Teste",
                "ACTIVE"
        ));

        CompanyRecord company = companyRepository.save(new CompanyRecord(
                UUID.randomUUID(),
                TENANT_ALPHA_ID,
                "Alpha Idemp Empresa LTDA",
                "Alpha Idemp",
                "99888777000166",
                "111222333444",
                "SIMPLES_NACIONAL",
                true,
                "Vínculo Idempotência",
                Instant.now(),
                Instant.now()
        ));

        companyAccountLinkRepository.save(new CompanyAccountLinkRecord(
                UUID.randomUUID(),
                TENANT_ALPHA_ID,
                company.getId(),
                account.getId(),
                Instant.now().minusSeconds(86400),
                null,
                true,
                1,
                "Vínculo Idempotência",
                "admin@seller.com",
                Instant.now(),
                Instant.now()
        ));

        Order order = orderRepository.save(new Order(
                UUID.randomUUID(),
                TENANT_ALPHA_ID,
                account.getId(),
                "MERCADO_LIVRE",
                "MLB-ORD-IDEMP-001",
                OrderStatus.PAID,
                new BigDecimal("150.0000"),
                new BigDecimal("10.0000"),
                new BigDecimal("160.0000"),
                "BRL",
                "Comprador Idempotente",
                "111.222.333-44",
                "BR-IDEMP-TRACK",
                null,
                null,
                null,
                "{}",
                "v1",
                "MERCADO_LIVRE_API",
                Instant.now(),
                Instant.now()
        ));

        orderItemRepository.save(new OrderItem(
                UUID.randomUUID(),
                TENANT_ALPHA_ID,
                order.getId(),
                null,
                null,
                "MLB-LISTING-IDEMP",
                "MLB-ITM-IDEMP-01",
                "SKU-IDEMP-01",
                "Mousepad Gamer",
                new BigDecimal("150.0000"),
                1,
                new BigDecimal("150.0000")
        ));

        // Executa a apuração 3 vezes consecutivas (simulando retries de rede/mensageria)
        var fact1 = orderFinancialFactService.computeOrReprocessOrderFacts(TENANT_ALPHA_ID, order.getId());
        var fact2 = orderFinancialFactService.computeOrReprocessOrderFacts(TENANT_ALPHA_ID, order.getId());
        var fact3 = orderFinancialFactService.computeOrReprocessOrderFacts(TENANT_ALPHA_ID, order.getId());

        // Verifica que todos os retornos são idênticos
        assertThat(fact1.grossRevenue()).isEqualByComparingTo(fact2.grossRevenue());
        assertThat(fact2.grossRevenue()).isEqualByComparingTo(fact3.grossRevenue());
        assertThat(fact1.netRevenue()).isEqualByComparingTo(fact3.netRevenue());
        assertThat(fact1.contributionMarginAmount()).isEqualByComparingTo(fact3.contributionMarginAmount());

        // Verifica no repositório que existe exatamente 1 fato persistido (zero duplicidade de faturamento)
        var factsInDb = orderFinancialFactRepository.findByTenantIdAndOrderId(TENANT_ALPHA_ID, order.getId());
        assertThat(factsInDb).isPresent();
    }

    // =========================================================================================
    // CENÁRIO 6: Dead Letter Queue (DLQ) para Descarte Seguro de Cargas Tóxicas
    // =========================================================================================
    @Test
    @DisplayName("Cenário 6: Payload de pedido corrompido é desviado para a DLQ sem travar a esteira")
    void testCorruptedOrderPayloadRoutedToDlq() throws Exception {
        SellerAccount account = sellerAccountRepository.save(new SellerAccount(
                UUID.randomUUID(),
                TENANT_ALPHA_ID,
                "MERCADO_LIVRE",
                "SELLER_DLQ_01",
                "Loja DLQ Teste",
                "ACTIVE"
        ));

        FakeMarketplaceAdapter fakeAdapter = FakeMarketplaceAdapter.createDefault();

        // Cria lote contendo 1 pedido válido e 1 pedido tóxico (externalOrderId nulo)
        RawOrder validOrder = new RawOrder(
                "MLB-VALID-DLQ-001",
                account.getId(),
                "MERCADO_LIVRE",
                OrderStatus.PAID,
                new BigDecimal("100.0000"),
                BigDecimal.ZERO,
                new BigDecimal("100.0000"),
                "BRL",
                "Cliente Válido",
                "111.222.333-44",
                null,
                null,
                null,
                null,
                Instant.now(),
                "{\"valid\": true}",
                "v1",
                "MELI_API",
                List.of(new RawOrderItem("ITM-01", "MLB-LST-01", "SKU-01", "Item Válido", new BigDecimal("100.0000"), 1, new BigDecimal("100.0000")))
        );

        RawOrder toxicOrder = new RawOrder(
                null, // Toxic: chave externa ausente
                account.getId(),
                "MERCADO_LIVRE",
                OrderStatus.PAID,
                new BigDecimal("50.0000"),
                BigDecimal.ZERO,
                new BigDecimal("50.0000"),
                "BRL",
                "Cliente Tóxico",
                "000.000.000-00",
                null,
                null,
                null,
                null,
                Instant.now(),
                "{\"corrupted_payload\": true}",
                "v1",
                "MELI_API",
                List.of()
        );

        OrderSyncResult syncResult = orderSyncService.syncOwnOrders(
                TENANT_ALPHA_ID,
                account.getId(),
                "MERCADO_LIVRE",
                50,
                fakeAdapter,
                (req, ctx) -> new OrderPageResponse(List.of(validOrder, toxicOrder), null, false, 2)
        );

        // Valida que a sincronização não explodiu: 1 pedido criado, 1 desviado para a DLQ
        assertThat(syncResult.ordersCreated()).isEqualTo(1);
        assertThat(syncResult.dlqCount()).isEqualTo(1);

        // Consulta a DLQ via endpoint REST oficial
        mockMvc.perform(get("/api/v1/orders/dlq")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAlpha)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[?(@.errorReason =~ /.*externalOrderId ausente.*/)]").exists());
    }

    // =========================================================================================
    // CENÁRIO 7: Duplicidade de Exportação (Idempotência por Chave)
    // =========================================================================================
    @Test
    @DisplayName("Cenário 7: Requisições repetidas de exportação com a mesma chave retornam o mesmo job sem duplicação")
    void testExportRequestWithSameIdempotencyKeyReturnsSameJob() throws Exception {
        String sharedIdempotencyKey = "idemp-key-dup-test-777";

        ExportRequestDto request = new ExportRequestDto(
                ExportDataset.FINANCIAL_FACTS,
                ExportFileFormat.CSV,
                sharedIdempotencyKey,
                100,
                Map.of("marketplace", "MERCADO_LIVRE")
        );

        // 1ª Requisição
        MvcResult result1 = mockMvc.perform(post("/api/v1/reports/exports")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAlpha)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andReturn();

        ExportJobResponseDto job1 = objectMapper.readValue(
                result1.getResponse().getContentAsString(),
                ExportJobResponseDto.class
        );

        // 2ª Requisição com a mesma chave
        MvcResult result2 = mockMvc.perform(post("/api/v1/reports/exports")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAlpha)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andReturn();

        ExportJobResponseDto job2 = objectMapper.readValue(
                result2.getResponse().getContentAsString(),
                ExportJobResponseDto.class
        );

        // Valida que o job retornado é exatamente o mesmo
        assertThat(job1.id()).isEqualTo(job2.id());
        assertThat(job1.downloadToken()).isEqualTo(job2.downloadToken());
    }

    // =========================================================================================
    // CENÁRIO 8: Isolamento Estrito Cross-Tenant (Segurança em Profundidade)
    // =========================================================================================
    @Test
    @DisplayName("Cenário 8: Tentativas de acesso cross-tenant entre Tenant Beta e recursos do Tenant Alpha são bloqueadas")
    void testStrictCrossTenantIsolationEnforced() throws Exception {
        // 1. Cria recursos de propriedade exclusiva do Tenant Alpha
        SellerAccount accountAlpha = sellerAccountRepository.save(new SellerAccount(
                UUID.randomUUID(),
                TENANT_ALPHA_ID,
                "MERCADO_LIVRE",
                "ACC_ALPHA_PRIVATE",
                "Alpha Loja Secreta",
                "ACTIVE"
        ));

        Order orderAlpha = orderRepository.save(new Order(
                UUID.randomUUID(),
                TENANT_ALPHA_ID,
                accountAlpha.getId(),
                "MERCADO_LIVRE",
                "MLB-ORD-SECRET-ALPHA",
                OrderStatus.PAID,
                new BigDecimal("500.0000"),
                BigDecimal.ZERO,
                new BigDecimal("500.0000"),
                "BRL",
                "Cliente Privado",
                "000.111.222-33",
                null,
                null,
                null,
                null,
                "{}",
                "v1",
                "MELI_API",
                Instant.now(),
                Instant.now()
        ));

        MarketplaceConnection connectionAlpha = marketplaceConnectionRepository.save(new MarketplaceConnection(
                UUID.randomUUID(),
                TENANT_ALPHA_ID,
                "MERCADO_LIVRE",
                "SELLER_ALPHA_CONN",
                "Alpha Loja ML",
                "encrypted-token-alpha",
                "encrypted-refresh-alpha",
                "ACTIVE"
        ));

        ExportRequestDto exportReqAlpha = new ExportRequestDto(
                ExportDataset.ORDERS,
                ExportFileFormat.CSV,
                "idemp-cross-tenant-alpha",
                100,
                Map.of()
        );

        MvcResult exportAlphaResult = mockMvc.perform(post("/api/v1/reports/exports")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAlpha)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exportReqAlpha)))
                .andExpect(status().isAccepted())
                .andReturn();

        ExportJobResponseDto exportJobAlpha = objectMapper.readValue(
                exportAlphaResult.getResponse().getContentAsString(),
                ExportJobResponseDto.class
        );

        // =====================================================================================
        // TENTATIVAS DE INVASÃO PELO TENANT BETA
        // =====================================================================================

        // 8a: Tenant Beta tenta consultar o pedido privado do Tenant Alpha -> 404 Not Found
        mockMvc.perform(get("/api/v1/orders/" + orderAlpha.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenBeta)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        // 8b: Tenant Beta tenta consultar a conexão de marketplace do Tenant Alpha -> 404 Not Found
        mockMvc.perform(get("/api/v1/integrations/mercadolivre/connections/" + connectionAlpha.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenBeta)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        // 8c: Tenant Beta tenta fazer download do relatório do Tenant Alpha -> 403 Forbidden
        mockMvc.perform(get("/api/v1/reports/exports/" + exportJobAlpha.id() + "/download")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenBeta)
                        .param("token", exportJobAlpha.downloadToken()))
                .andExpect(status().isForbidden());

        // 8d: Dashboard de KPIs do Tenant Beta não deve conter os valores do Tenant Alpha (isolamento analítico)
        mockMvc.perform(get("/api/v1/analytics/kpis/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenBeta)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders", is(0)))
                .andExpect(jsonPath("$.totalGrossRevenue", is(0.0)))
                .andExpect(jsonPath("$.totalNetRevenue", is(0.0)));
    }
}
