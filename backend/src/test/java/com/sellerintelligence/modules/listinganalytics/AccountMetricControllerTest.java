package com.sellerintelligence.modules.listinganalytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellerintelligence.core.security.JwtTokenProvider;
import com.sellerintelligence.modules.catalog.SellerAccount;
import com.sellerintelligence.modules.catalog.SellerAccountRepository;
import com.sellerintelligence.modules.listinganalytics.model.IngestMetricsCommand;
import com.sellerintelligence.modules.listinganalytics.model.MetricGranularity;
import com.sellerintelligence.modules.listinganalytics.model.MetricScope;
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
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
class AccountMetricControllerTest {

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private SellerAccountRepository sellerAccountRepository;

    private String token;
    private SellerAccount sellerAccount;
    private Instant windowStart;
    private Instant windowEnd;

    @BeforeEach
    void setUp() {
        token = jwtTokenProvider.generateToken(
                USER_ID, "metrics-tester@sellerintelligence.com", TENANT_ID, List.of("ROLE_ADMIN")
        );

        sellerAccount = sellerAccountRepository.save(new SellerAccount(
                UUID.randomUUID(),
                TENANT_ID,
                "MERCADO_LIVRE",
                "MLB-ACC-METRICS",
                "Loja de Métricas ML",
                "ACTIVE"
        ));

        windowStart = Instant.now().minus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
        windowEnd = Instant.now().truncatedTo(ChronoUnit.DAYS);
    }

    @Test
    @DisplayName("GET /api/v1/metrics/catalog: Deve retornar catálogo oficial documentado com limites e status")
    void shouldReturnOfficialMetricCatalog() throws Exception {
        mockMvc.perform(get("/api/v1/metrics/catalog")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[?(@.metricKey == 'VISITS')].supportedByChannel", hasItem(true)))
                .andExpect(jsonPath("$[?(@.metricKey == 'IMPRESSIONS' && @.marketplace == 'MERCADO_LIVRE')].supportedByChannel", hasItem(false)))
                .andExpect(jsonPath("$[?(@.metricKey == 'COMPETITOR_SALES_VOLUME')].supportedByChannel", hasItem(false)));
    }

    @Test
    @DisplayName("GET /api/v1/metrics/catalog?marketplace=SHOPEE: Deve filtrar catálogo para a Shopee")
    void shouldReturnFilteredShopeeCatalog() throws Exception {
        mockMvc.perform(get("/api/v1/metrics/catalog")
                        .param("marketplace", "SHOPEE")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.metricKey == 'PAGE_VIEWS')]", not(empty())))
                .andExpect(jsonPath("$[?(@.metricKey == 'UNIQUE_VISITORS')]", not(empty())));
    }

    @Test
    @DisplayName("POST /api/v1/metrics/ingest: Deve ingerir métricas oficiais e derivar taxas com status 201 Created")
    void shouldIngestMetricsSuccessfully() throws Exception {
        Map<String, BigDecimal> metrics = new HashMap<>();
        metrics.put("VISITS", new BigDecimal("1200"));
        metrics.put("ORDERS_COUNT", new BigDecimal("36"));
        metrics.put("UNITS_SOLD", new BigDecimal("48"));
        metrics.put("REVENUE", new BigDecimal("5400.00"));

        IngestMetricsCommand command = new IngestMetricsCommand(
                sellerAccount.getId(),
                "MERCADO_LIVRE",
                "MLB-ITEM-1234",
                MetricScope.LISTING,
                MetricGranularity.DAILY,
                windowStart,
                windowEnd,
                metrics,
                "/items/MLB-ITEM-1234/visits/time_window"
        );

        mockMvc.perform(post("/api/v1/metrics/ingest")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[?(@.metricKey == 'VISITS')].metricValue", hasItem(1200)))
                .andExpect(jsonPath("$[?(@.metricKey == 'CONVERSION_RATE_ORDERS')].metricValue", hasItem(notNullValue())))
                .andExpect(jsonPath("$[?(@.metricKey == 'CONVERSION_RATE_UNITS')].metricValue", hasItem(notNullValue())))
                .andExpect(jsonPath("$[?(@.metricKey == 'CTR')].availabilityStatus", hasItem("UNSUPPORTED_BY_CHANNEL")));
    }

    @Test
    @DisplayName("GET /api/v1/metrics: Deve listar métricas persistidas com paginação e filtros")
    void shouldSearchMetricsWithFilters() throws Exception {
        // Primeiro ingere
        Map<String, BigDecimal> metrics = Map.of(
                "VISITS", new BigDecimal("800"),
                "ORDERS_COUNT", new BigDecimal("20")
        );
        IngestMetricsCommand command = new IngestMetricsCommand(
                sellerAccount.getId(),
                "MERCADO_LIVRE",
                null,
                MetricScope.ACCOUNT,
                MetricGranularity.DAILY,
                windowStart,
                windowEnd,
                metrics,
                "/users/items_visits"
        );

        mockMvc.perform(post("/api/v1/metrics/ingest")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated());

        // Busca
        mockMvc.perform(get("/api/v1/metrics")
                        .header("Authorization", "Bearer " + token)
                        .param("sellerAccountId", sellerAccount.getId().toString())
                        .param("marketplace", "MERCADO_LIVRE")
                        .param("metricKey", "VISITS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", not(empty())))
                .andExpect(jsonPath("$.content[0].metricKey", is("VISITS")))
                .andExpect(jsonPath("$.content[0].metricValue", is(800)));
    }

    @Test
    @DisplayName("GET /api/v1/metrics/summary: Deve retornar resumo consolidado com aviso anti-alucinação")
    void shouldReturnMetricsSummary() throws Exception {
        // Ingesta dados
        Map<String, BigDecimal> metrics = Map.of(
                "VISITS", new BigDecimal("1000"),
                "ORDERS_COUNT", new BigDecimal("50"),
                "UNITS_SOLD", new BigDecimal("70"),
                "REVENUE", new BigDecimal("3500.00")
        );
        IngestMetricsCommand command = new IngestMetricsCommand(
                sellerAccount.getId(),
                "MERCADO_LIVRE",
                null,
                MetricScope.ACCOUNT,
                MetricGranularity.DAILY,
                windowStart,
                windowEnd,
                metrics,
                "/users/items_visits"
        );

        mockMvc.perform(post("/api/v1/metrics/ingest")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(command)))
                .andExpect(status().isCreated());

        // Consulta resumo
        mockMvc.perform(get("/api/v1/metrics/summary")
                        .header("Authorization", "Bearer " + token)
                        .param("sellerAccountId", sellerAccount.getId().toString())
                        .param("marketplace", "MERCADO_LIVRE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVisits", is(1000)))
                .andExpect(jsonPath("$.totalOrders", is(50)))
                .andExpect(jsonPath("$.overallConversionRateOrders", notNullValue()))
                .andExpect(jsonPath("$.antiHallucinationNotice", containsString("Métricas oficiais sincronizadas")));
    }
}
