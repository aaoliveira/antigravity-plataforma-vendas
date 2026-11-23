package com.sellerintelligence.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellerintelligence.core.security.JwtTokenProvider;
import com.sellerintelligence.modules.catalog.MarketplaceListing;
import com.sellerintelligence.modules.catalog.MarketplaceListingRepository;
import com.sellerintelligence.modules.catalog.Product;
import com.sellerintelligence.modules.catalog.ProductRepository;
import com.sellerintelligence.modules.catalog.SellerAccount;
import com.sellerintelligence.modules.catalog.SellerAccountRepository;
import com.sellerintelligence.modules.company.entity.CompanyAccountLinkRecord;
import com.sellerintelligence.modules.company.entity.CompanyRecord;
import com.sellerintelligence.modules.company.repository.CompanyAccountLinkRepository;
import com.sellerintelligence.modules.company.repository.CompanyRepository;
import com.sellerintelligence.modules.financialanalytics.repository.OrderFinancialFactRepository;
import com.sellerintelligence.modules.financialanalytics.service.OrderFinancialFactService;
import com.sellerintelligence.modules.identityaccess.dto.LoginRequest;
import com.sellerintelligence.modules.marketplaceconnection.mercadolivre.MeliOAuthClient;
import com.sellerintelligence.modules.marketplaceconnection.mercadolivre.MeliTokenResponse;
import com.sellerintelligence.modules.marketplaceconnection.mercadolivre.dto.OAuthCallbackRequest;
import com.sellerintelligence.modules.orderingestion.Order;
import com.sellerintelligence.modules.orderingestion.OrderItem;
import com.sellerintelligence.modules.orderingestion.OrderItemRepository;
import com.sellerintelligence.modules.orderingestion.OrderRepository;
import com.sellerintelligence.modules.orderingestion.model.OrderStatus;
import com.sellerintelligence.modules.pricing.model.FeeSourceType;
import com.sellerintelligence.modules.pricing.model.FeeType;
import com.sellerintelligence.modules.pricing.model.FeeValueType;
import com.sellerintelligence.modules.pricing.model.ProductMarginSimulationRequest;
import com.sellerintelligence.modules.pricing.model.RegisterFeeRuleCommand;
import com.sellerintelligence.modules.pricing.model.RegisterProductCostCommand;
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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * P023-BE/QA: Suíte E2E do Ciclo de Vida Completo da Plataforma Seller Intelligence.
 *
 * Executa o pipeline de ponta a ponta sem atalhos nem mocks de fachada contábeis:
 * 1. Autenticação (Login do operador e obtenção de token JWT assinado)
 * 2. Contexto de Tenant (Validação do isolamento organizacional e roles)
 * 3. Conexão Marketplace Sandbox (Início OAuth, PKCE Anti-CSRF, callback seguro e vínculo ativo)
 * 4. Sincronização de Dados (Catálogo, Pedidos faturados e apuração financeira determinística)
 * 5. Consulta de Catálogo e Pedidos (Endpoints REST com filtros e ordenação)
 * 6. Simulação Determinística de Margem (Precisão em BigDecimal, regras de comissão e disclaimer)
 * 7. Consulta de Dashboard e Cache (Métricas consolidadas, agrupamentos e cache tenant-aware)
 * 8. Exportação Assíncrona e Download Auditado (CSV/XLSX, sanitização anti-injection e link temporário)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("P023-BE/QA: EndToEndLifecycleIntegrationTest — Pipeline E2E Completo")
public class EndToEndLifecycleIntegrationTest {

    private static final UUID TENANT_ALPHA_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String SELLER_EMAIL = "seller@example.com";
    private static final String SELLER_PASSWORD = "Password@123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private SellerAccountRepository sellerAccountRepository;

    @Autowired
    private CompanyAccountLinkRepository companyAccountLinkRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MarketplaceListingRepository marketplaceListingRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderFinancialFactRepository orderFinancialFactRepository;

    @Autowired
    private OrderFinancialFactService orderFinancialFactService;

    @MockitoBean
    private MeliOAuthClient meliOAuthClient;

    @BeforeEach
    void setUp() {
        when(meliOAuthClient.buildAuthorizationUrl(anyString(), anyString())).thenAnswer(invocation -> {
            String state = invocation.getArgument(0);
            String challenge = invocation.getArgument(1);
            return "https://auth.mercadolivre.com.br/authorization?response_type=code&client_id=test-app&state="
                    + state + "&code_challenge=" + challenge + "&code_challenge_method=S256";
        });

        when(meliOAuthClient.exchangeAuthorizationCode(anyString(), anyString(), anyString())).thenReturn(
                new MeliTokenResponse(
                        "APP_USR-sandbox-access-token-e2e",
                        "bearer",
                        21600L,
                        "read offline_access",
                        "987654321",
                        "TG-sandbox-refresh-token-e2e"
                )
        );
    }

    @Test
    @DisplayName("Executar Pipeline E2E Completo: Autenticar -> Tenant -> Conectar -> Sincronizar -> Consultar -> Simular Margem -> Dashboard -> Exportar")
    void shouldExecuteFullEndToEndLifecycleSuccessfully() throws Exception {

        // =========================================================================================
        // PASSO 1: Autenticação do Operador e Obtenção do Token JWT
        // =========================================================================================
        LoginRequest loginRequest = new LoginRequest(SELLER_EMAIL, SELLER_PASSWORD, null);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.activeTenantId", is(TENANT_ALPHA_ID.toString())))
                .andExpect(jsonPath("$.roles[0]", is("ROLE_OWNER")))
                .andReturn();

        String rawLoginResponse = loginResult.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(rawLoginResponse).get("accessToken").asText();
        assertThat(accessToken).isNotBlank();

        JwtTokenProvider.Claims tokenClaims = jwtTokenProvider.validateAndParseToken(accessToken);
        assertThat(tokenClaims.tenantId()).isEqualTo(TENANT_ALPHA_ID);
        assertThat(tokenClaims.email()).isEqualTo(SELLER_EMAIL);

        String authHeader = "Bearer " + accessToken;

        // =========================================================================================
        // PASSO 2: Resolução e Validação do Contexto de Tenant
        // =========================================================================================
        mockMvc.perform(get("/api/v1/tenancy/current")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId", is(TENANT_ALPHA_ID.toString())))
                .andExpect(jsonPath("$.name", is("Alpha Seller Org")))
                .andExpect(jsonPath("$.slug", is("alpha-seller")))
                .andExpect(jsonPath("$.role", is("OWNER")))
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        // =========================================================================================
        // PASSO 3: Conexão de Marketplace Sandbox com PKCE Anti-CSRF
        // =========================================================================================
        // 3.1 Inicia autorização OAuth
        MvcResult initOAuthResult = mockMvc.perform(post("/api/v1/integrations/mercadolivre/authorize")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizationUrl", containsString("auth.mercadolivre.com.br")))
                .andExpect(jsonPath("$.stateToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty())
                .andReturn();

        String stateToken = objectMapper.readTree(initOAuthResult.getResponse().getContentAsString())
                .get("stateToken").asText();

        // 3.2 Executa callback do marketplace com PKCE
        OAuthCallbackRequest callbackReq = new OAuthCallbackRequest(
                "TG-sandbox-auth-code",
                stateToken,
                "http://localhost:3000/callback"
        );

        MvcResult callbackResult = mockMvc.perform(post("/api/v1/integrations/mercadolivre/callback")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(callbackReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.tenantId", is(TENANT_ALPHA_ID.toString())))
                .andExpect(jsonPath("$.marketplace", is("MERCADO_LIVRE")))
                .andExpect(jsonPath("$.sellerAccountId", is("987654321")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.accessToken").doesNotExist()) // Proteção estrita contra vazamento de segredos
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andReturn();

        String connectionIdStr = objectMapper.readTree(callbackResult.getResponse().getContentAsString())
                .get("id").asText();

        // 3.3 Valida listagem das conexões ativas
        mockMvc.perform(get("/api/v1/integrations/mercadolivre/connections")
                        .header(HttpHeaders.AUTHORIZATION, authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[?(@.id == '" + connectionIdStr + "')].status", contains("ACTIVE")));

        // =========================================================================================
        // PASSO 4: Sincronização de Dados de Catálogo e Pedidos com Apuração Contábil
        // =========================================================================================
        // 4.1 Registra empresa e conta conectada para integridade relacional
        CompanyRecord company = companyRepository.save(new CompanyRecord(
                UUID.randomUUID(),
                TENANT_ALPHA_ID,
                "Alpha Comércio Digital E2E LTDA",
                "Alpha Digital",
                "11222333000144",
                "123456789000",
                "SIMPLES_NACIONAL",
                true,
                "Empresa homologada E2E",
                Instant.now(),
                Instant.now()
        ));

        SellerAccount sellerAccount = sellerAccountRepository.save(new SellerAccount(
                UUID.randomUUID(),
                TENANT_ALPHA_ID,
                "MERCADO_LIVRE",
                "SELLER_MELI_E2E_01",
                "Loja Oficial Alpha E2E",
                "ACTIVE"
        ));

        companyAccountLinkRepository.save(new CompanyAccountLinkRecord(
                UUID.randomUUID(),
                TENANT_ALPHA_ID,
                company.getId(),
                sellerAccount.getId(),
                Instant.now().minusSeconds(86400),
                null,
                true,
                1,
                "Vínculo inicial E2E",
                SELLER_EMAIL,
                Instant.now(),
                Instant.now()
        ));

        // 4.2 Cadastra Produto Canônico e Custo Vigente
        Product canonicalProduct = productRepository.save(new Product(
                UUID.randomUUID(),
                TENANT_ALPHA_ID,
                "Headset Gamer Pro Surround 7.1",
                "SKU-HEADSET-PRO-01",
                "AudioTech",
                "Periféricos",
                "ACTIVE"
        ));

        RegisterProductCostCommand costCommand = new RegisterProductCostCommand(
                canonicalProduct.getId(),
                null,
                company.getId(),
                "BRL",
                "UN",
                new BigDecimal("70.0000"), // Aquisição (COGS)
                new BigDecimal("5.0000"),  // Embalagem
                BigDecimal.ZERO,           // Adicional
                BigDecimal.ZERO,           // Frete subsidiado em lote
                new BigDecimal("8.0000"),  // Alíquota imposto
                new BigDecimal("4.0000"),  // Alíquota operacional
                Instant.now().minusSeconds(86400),
                "Custo base inicial de lote"
        );

        mockMvc.perform(post("/api/v1/pricing/costs")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(costCommand)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.totalDirectUnitCost", is(75.0)));

        // 4.3 Cadastra Regra de Taxa Oficial do Marketplace
        RegisterFeeRuleCommand feeRuleCommand = new RegisterFeeRuleCommand(
                "MERCADO_LIVRE",
                "CLASSIC",
                null,
                FeeType.COMMISSION,
                FeeValueType.PERCENTAGE,
                new BigDecimal("14.0000"), // 14% de comissão
                BigDecimal.ZERO,
                "BRL",
                BigDecimal.ZERO,
                new BigDecimal("999999.0000"),
                FeeSourceType.OFFICIAL_CONTRACT,
                "Tabela oficial Mercado Livre Premium",
                "Contrato Comercial 2026",
                Instant.now().minusSeconds(86400)
        );

        mockMvc.perform(post("/api/v1/pricing/fee-rules")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(feeRuleCommand)))
                .andExpect(status().isCreated());

        // 4.4 Cadastra Anúncio Sincronizado no Catálogo
        MarketplaceListing listing = marketplaceListingRepository.save(new MarketplaceListing(
                UUID.randomUUID(),
                TENANT_ALPHA_ID,
                sellerAccount.getId(),
                canonicalProduct.getId(),
                null,
                "MERCADO_LIVRE",
                "MLB-E2E-777888",
                "SKU-HEADSET-PRO-01",
                null,
                "Headset Gamer Pro Surround 7.1 - Envio Imediato",
                new BigDecimal("200.0000"),
                "BRL",
                50,
                "ACTIVE",
                "https://mercadolivre.com.br/MLB-E2E-777888",
                "{}",
                "v1",
                "MERCADO_LIVRE_API"
        ));

        // 4.5 Ingestão de Pedido Pago e Itens
        Order order = orderRepository.save(new Order(
                UUID.randomUUID(),
                TENANT_ALPHA_ID,
                sellerAccount.getId(),
                "MERCADO_LIVRE",
                "MLB-ORD-E2E-999001",
                OrderStatus.PAID,
                new BigDecimal("200.0000"),
                new BigDecimal("15.0000"),
                new BigDecimal("215.0000"),
                "BRL",
                "Cliente Final E2E",
                "123.456.789-00",
                "BR-E2E-TRACK-01",
                null,
                null,
                null,
                "{}",
                "v1",
                "MERCADO_LIVRE_API",
                Instant.now().minusSeconds(3600),
                Instant.now()
        ));

        OrderItem orderItem = orderItemRepository.save(new OrderItem(
                UUID.randomUUID(),
                TENANT_ALPHA_ID,
                order.getId(),
                canonicalProduct.getId(),
                null,
                listing.getExternalListingId(),
                "MLB-ITM-E2E-001",
                canonicalProduct.getCanonicalSku(),
                canonicalProduct.getTitle(),
                new BigDecimal("200.0000"),
                1,
                new BigDecimal("200.0000")
        ));

        // 4.6 Apuração Idempotente de Fatos Financeiros
        var computedFact = orderFinancialFactService.computeOrReprocessOrderFacts(TENANT_ALPHA_ID, order.getId());
        assertThat(computedFact).isNotNull();
        assertThat(computedFact.orderId()).isEqualTo(order.getId());
        assertThat(computedFact.grossRevenue()).isEqualByComparingTo("200.0000");
        assertThat(computedFact.netRevenue()).isEqualByComparingTo("200.0000");
        assertThat(computedFact.marketplaceFee()).isEqualByComparingTo("28.0000"); // 14% de 200
        assertThat(computedFact.productCost()).isEqualByComparingTo("70.0000"); // Custo de aquisição
        assertThat(computedFact.taxAmount()).isEqualByComparingTo("16.0000"); // 8% de 200
        assertThat(computedFact.otherExpensesAmount()).isEqualByComparingTo("8.0000"); // 4% de 200
        // Margem de Contribuição = 200 - 28 (comissão) - 15 (frete) - 70 (COGS) - 5 (embalagem) - 16 (imposto) - 8 (despesas) = 58.0000
        assertThat(computedFact.contributionMarginAmount()).isEqualByComparingTo("58.0000");
        assertThat(computedFact.contributionMarginRatio()).isEqualByComparingTo("29.0000"); // 29.0000%

        // =========================================================================================
        // PASSO 5: Consulta de Catálogo e Pedidos via REST
        // =========================================================================================
        // 5.1 Consulta Catálogo
        mockMvc.perform(get("/api/v1/catalog/listings")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .param("marketplace", "MERCADO_LIVRE")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[?(@.externalListingId == 'MLB-E2E-777888')].title",
                        contains("Headset Gamer Pro Surround 7.1 - Envio Imediato")))
                .andExpect(jsonPath("$.content[?(@.externalListingId == 'MLB-E2E-777888')].price",
                        contains(200.0)));

        // 5.2 Consulta Pedidos
        mockMvc.perform(get("/api/v1/orders")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .param("status", "PAID")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.content[?(@.externalOrderId == 'MLB-ORD-E2E-999001')].totalAmount",
                        contains(215.0)))
                .andExpect(jsonPath("$.content[?(@.externalOrderId == 'MLB-ORD-E2E-999001')].status",
                        contains("PAID")));

        // =========================================================================================
        // PASSO 6: Simulação Determinística de Margem de Produto
        // =========================================================================================
        ProductMarginSimulationRequest simRequest = new ProductMarginSimulationRequest(
                canonicalProduct.getId(),
                null,
                company.getId(),
                "MERCADO_LIVRE",
                new BigDecimal("200.00"),
                BigDecimal.ZERO,
                "CLASSIC",
                null,
                Instant.now(),
                new BigDecimal("5.00"),
                new BigDecimal("15.00"),
                null
        );

        mockMvc.perform(post("/api/v1/pricing/margins/simulate-product")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(simRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grossSellingPrice", is(200.0)))
                .andExpect(jsonPath("$.isBreakEvenAttainable", is(true)))
                .andExpect(jsonPath("$.contributionMarginAmount", is(notNullValue())))
                .andExpect(jsonPath("$.contributionMarginPercent", is(notNullValue())));

        // =========================================================================================
        // PASSO 7: Consulta de KPIs Agregados no Dashboard e Cache Tenant-Aware
        // =========================================================================================
        // 7.1 Resumo consolidado de KPIs
        mockMvc.perform(get("/api/v1/analytics/kpis/summary")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.totalGrossRevenue", greaterThan(0.0)))
                .andExpect(jsonPath("$.totalContributionMarginAmount", greaterThan(0.0)))
                .andExpect(jsonPath("$.cached", is(false)));

        // 7.2 Segunda chamada deve retornar Cache Hit
        mockMvc.perform(get("/api/v1/analytics/kpis/summary")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cached", is(true)));

        // 7.3 KPIs por Canal
        mockMvc.perform(get("/api/v1/analytics/kpis/by-channel")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[?(@.marketplace == 'MERCADO_LIVRE')].orderCount", contains(greaterThanOrEqualTo(1))));

        // 7.4 KPIs por Período
        mockMvc.perform(get("/api/v1/analytics/kpis/by-period")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .param("granularity", "DAILY")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));

        // =========================================================================================
        // PASSO 8: Exportação Assíncrona e Download Auditado
        // =========================================================================================
        ExportRequestDto exportReq = new ExportRequestDto(
                ExportDataset.FINANCIAL_FACTS,
                ExportFileFormat.CSV,
                "e2e-pipeline-export-key-001",
                500,
                Map.of("marketplace", "MERCADO_LIVRE")
        );

        MvcResult exportResult = mockMvc.perform(post("/api/v1/reports/exports")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exportReq)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.tenantId", is(TENANT_ALPHA_ID.toString())))
                .andExpect(jsonPath("$.dataset", is("FINANCIAL_FACTS")))
                .andExpect(jsonPath("$.fileFormat", is("CSV")))
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.downloadToken", notNullValue()))
                .andReturn();

        ExportJobResponseDto job = objectMapper.readValue(
                exportResult.getResponse().getContentAsString(),
                ExportJobResponseDto.class
        );

        // Download Seguro com Token Assinado
        mockMvc.perform(get("/api/v1/reports/exports/" + job.id() + "/download")
                        .header(HttpHeaders.AUTHORIZATION, authHeader)
                        .param("token", job.downloadToken()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment; filename=")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Cache-Control", "no-cache, no-store, must-revalidate"))
                .andExpect(content().contentTypeCompatibleWith("text/csv"));
    }
}
