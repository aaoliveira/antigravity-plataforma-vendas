package com.sellerintelligence.modules.financialanalytics.aggregation;

import com.sellerintelligence.core.security.JwtTokenProvider;
import com.sellerintelligence.modules.catalog.SellerAccount;
import com.sellerintelligence.modules.catalog.SellerAccountRepository;
import com.sellerintelligence.modules.company.entity.CompanyRecord;
import com.sellerintelligence.modules.company.repository.CompanyRepository;
import com.sellerintelligence.modules.financialanalytics.entity.OrderFinancialFactRecord;
import com.sellerintelligence.modules.financialanalytics.model.FinancialValueProvenance;
import com.sellerintelligence.modules.financialanalytics.model.ReconciliationStatus;
import com.sellerintelligence.modules.financialanalytics.repository.OrderFinancialFactRepository;
import com.sellerintelligence.modules.orderingestion.Order;
import com.sellerintelligence.modules.orderingestion.OrderRepository;
import com.sellerintelligence.modules.orderingestion.model.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AggregatedKpiController - Testes de Integração REST e Segurança de Tenant")
class AggregatedKpiControllerTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private SellerAccountRepository sellerAccountRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderFinancialFactRepository factRepository;

    private String token;
    private CompanyRecord company;
    private SellerAccount sellerAccount;
    private Order order;

    @BeforeEach
    void setUp() {
        token = jwtTokenProvider.generateToken(
                USER_ID, "kpi-operator@seller.com", TENANT_ID, List.of("ROLE_ADMIN")
        );

        // Cria dependências relacionais para cumprir integridade referencial
        company = companyRepository.save(new CompanyRecord(
                UUID.randomUUID(),
                TENANT_ID,
                "Seller Corp Integracao",
                "Loja Alpha",
                "12345678000199",
                "123456",
                "SIMPLES",
                true,
                "Empresa de teste",
                Instant.now(),
                Instant.now()
        ));

        sellerAccount = sellerAccountRepository.save(new SellerAccount(
                UUID.randomUUID(),
                TENANT_ID,
                "MERCADO_LIVRE",
                "MLB-ACC-KPI-TEST",
                "Loja MLB Test",
                "ACTIVE"
        ));

        order = orderRepository.save(new Order(
                UUID.randomUUID(),
                TENANT_ID,
                sellerAccount.getId(),
                "MERCADO_LIVRE",
                "MLB-ORDER-100",
                OrderStatus.PAID,
                new BigDecimal("150.0000"),
                new BigDecimal("10.0000"),
                new BigDecimal("140.0000"),
                "BRL",
                "Cliente Teste",
                "12345678900",
                "TRACK100",
                null,
                null,
                null,
                "{}",
                "v1",
                "MERCADO_LIVRE_API",
                Instant.now(),
                Instant.now()
        ));

        // Insere fato financeiro para o tenant
        factRepository.save(new OrderFinancialFactRecord(
                UUID.randomUUID(),
                TENANT_ID,
                order.getId(),
                sellerAccount.getId(),
                company.getId(),
                "MERCADO_LIVRE",
                "MLB-ORDER-100",
                "PAID",
                Instant.now(),
                new BigDecimal("150.0000"),
                new BigDecimal("10.0000"),
                new BigDecimal("140.0000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("22.5000"),
                new BigDecimal("5.0000"),
                new BigDecimal("12.0000"),
                new BigDecimal("45.0000"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("8.0000"),
                BigDecimal.ZERO,
                new BigDecimal("92.5000"),
                new BigDecimal("47.5000"),
                new BigDecimal("33.9286"),
                "BRL",
                ReconciliationStatus.ESTIMATED,
                FinancialValueProvenance.ESTIMATED,
                FinancialValueProvenance.ESTIMATED,
                FinancialValueProvenance.CONFIRMED,
                FinancialValueProvenance.USER_INFORMED,
                false,
                "[]",
                "v1.0.0",
                "{}"
        ));
    }

    @Test
    @DisplayName("GET /api/v1/analytics/kpis/summary deve retornar resumo consolidado de KPIs com código 200")
    void shouldReturnKpiSummary() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/kpis/summary")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId", is(TENANT_ID.toString())))
                .andExpect(jsonPath("$.totalOrders", is(1)))
                .andExpect(jsonPath("$.totalGrossRevenue", is(150.0)))
                .andExpect(jsonPath("$.totalNetRevenue", is(140.0)))
                .andExpect(jsonPath("$.totalContributionMarginAmount", is(47.5)))
                .andExpect(jsonPath("$.averageOrderValue", is(150.0)))
                .andExpect(jsonPath("$.antiHallucinationDisclaimer", containsString("proibido rotular este valor como Lucro Líquido")))
                .andExpect(jsonPath("$.kpiDefinitions", hasSize(18)));
    }

    @Test
    @DisplayName("GET /api/v1/analytics/kpis/by-channel deve retornar agregação comparativa por canal")
    void shouldReturnKpisByChannel() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/kpis/by-channel")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].marketplace", is("MERCADO_LIVRE")))
                .andExpect(jsonPath("$[0].grossRevenue", is(150.0)))
                .andExpect(jsonPath("$[0].contributionMarginAmount", is(47.5)))
                .andExpect(jsonPath("$[0].revenueSharePercent", is(100.0)));
    }

    @Test
    @DisplayName("GET /api/v1/analytics/kpis/by-period deve retornar série temporal diária")
    void shouldReturnKpisByPeriod() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/kpis/by-period")
                        .param("granularity", "DAILY")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].orderCount", is(1)))
                .andExpect(jsonPath("$[0].grossRevenue", is(150.0)));
    }

    @Test
    @DisplayName("GET /api/v1/analytics/kpis/by-company deve retornar agregação por empresa")
    void shouldReturnKpisByCompany() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/kpis/by-company")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].grossRevenue", is(150.0)));
    }

    @Test
    @DisplayName("GET /api/v1/analytics/kpis/definitions deve retornar catálogo documentado das 18 definições oficiais")
    void shouldReturnKpiDefinitions() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/kpis/definitions")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(18)))
                .andExpect(jsonPath("$[*].code", hasItems(
                        "GROSS_REVENUE", "DISCOUNT_AMOUNT", "NET_REVENUE", "MARKETPLACE_FEES",
                        "PRODUCT_COST", "CONTRIBUTION_MARGIN", "CONTRIBUTION_MARGIN_RATIO", "AVERAGE_ORDER_VALUE"
                )));
    }

    @Test
    @DisplayName("POST /api/v1/analytics/kpis/cache/invalidate deve invalidar o cache com status 204 No Content")
    void shouldInvalidateTenantCache() throws Exception {
        mockMvc.perform(post("/api/v1/analytics/kpis/cache/invalidate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Requisição não autenticada deve retornar HTTP 401 Unauthorized")
    void shouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/kpis/summary")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
