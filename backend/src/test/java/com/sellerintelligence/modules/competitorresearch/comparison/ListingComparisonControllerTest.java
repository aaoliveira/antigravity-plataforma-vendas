package com.sellerintelligence.modules.competitorresearch.comparison;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellerintelligence.core.security.JwtTokenProvider;
import com.sellerintelligence.modules.competitorresearch.comparison.model.ComparableListingItem;
import com.sellerintelligence.modules.competitorresearch.comparison.model.CompareListingsRequest;
import com.sellerintelligence.modules.competitorresearch.comparison.model.FindEquivalentsRequest;
import com.sellerintelligence.modules.competitorresearch.comparison.model.ItemCondition;
import com.sellerintelligence.modules.competitorresearch.comparison.model.RecordPricePointRequest;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ListingComparisonControllerTest {

    private static final UUID TENANT_ALPHA_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TENANT_BETA_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID USER_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String tokenAlpha;
    private String tokenBeta;

    @BeforeEach
    void setUp() {
        tokenAlpha = jwtTokenProvider.generateToken(USER_ID, "alpha-admin@sellerintelligence.com", TENANT_ALPHA_ID, List.of("ROLE_ADMIN"));
        tokenBeta = jwtTokenProvider.generateToken(USER_ID, "beta-admin@sellerintelligence.com", TENANT_BETA_ID, List.of("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("POST /api/v1/market-comparison/compare: Deve comparar listings equivalentes com score explicável e frete")
    void shouldCompareListingsSuccessfully() throws Exception {
        ComparableListingItem base = ComparableListingItem.builder()
                .title("Monitor Dell 27 Polegadas 4K Ultra HD IPS S2722QC")
                .brand("Dell")
                .model("S2722QC")
                .gtin("0884116398905")
                .price(new BigDecimal("2499.00"))
                .shippingCost(new BigDecimal("30.00"))
                .observedAt(Instant.now().minus(2, ChronoUnit.DAYS))
                .condition(ItemCondition.NEW)
                .marketplace("MERCADO_LIVRE")
                .build();

        ComparableListingItem target = ComparableListingItem.builder()
                .title("Monitor Dell S2722QC 27 4K UHD IPS USB-C")
                .brand("Dell")
                .model("S2722QC")
                .gtin("0884116398905")
                .price(new BigDecimal("2399.00"))
                .shippingCost(new BigDecimal("40.00"))
                .observedAt(Instant.now())
                .condition(ItemCondition.NEW)
                .marketplace("SHOPEE")
                .build();

        CompareListingsRequest request = new CompareListingsRequest(
                base, target, null, null, new BigDecimal("0.7500"), 30, true
        );

        mockMvc.perform(post("/api/v1/market-comparison/compare")
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.similarity.isEquivalent", is(true)))
                .andExpect(jsonPath("$.similarity.isCompatible", is(true)))
                .andExpect(jsonPath("$.similarity.compatibilityStatus", is("COMPATIBLE")))
                .andExpect(jsonPath("$.similarity.gtinScore", is(1.0)))
                .andExpect(jsonPath("$.similarity.brandScore", is(1.0)))
                .andExpect(jsonPath("$.similarity.explanation", notNullValue()))
                .andExpect(jsonPath("$.priceComparison.isTemporalWindowComparable", is(true)))
                .andExpect(jsonPath("$.priceComparison.priceDifference", is(-100.0)))
                .andExpect(jsonPath("$.priceComparison.baseShippingStatus", is("KNOWN")))
                .andExpect(jsonPath("$.priceComparison.baseFinalPriceWithShipping", is(2529.0)))
                .andExpect(jsonPath("$.priceComparison.targetFinalPriceWithShipping", is(2439.0)))
                .andExpect(jsonPath("$.priceComparison.totalPriceDifferenceWithShipping", is(-90.0)));
    }

    @Test
    @DisplayName("POST /api/v1/market-comparison/compare: Deve sinalizar incompatibilidade e não inferir frete desconhecido")
    void shouldFlagIncompatibleAndNeverInferUnknownShipping() throws Exception {
        ComparableListingItem base = ComparableListingItem.builder()
                .title("Smartphone Samsung Galaxy S23 128GB Preto")
                .brand("Samsung")
                .price(new BigDecimal("3000.00"))
                .shippingCost(null) // Frete desconhecido!
                .condition(ItemCondition.NEW)
                .build();

        ComparableListingItem target = ComparableListingItem.builder()
                .title("Smartphone Apple iPhone 15 128GB Estelar")
                .brand("Apple") // Marca diferente!
                .price(new BigDecimal("4500.00"))
                .shippingCost(new BigDecimal("20.00"))
                .condition(ItemCondition.NEW)
                .build();

        CompareListingsRequest request = new CompareListingsRequest(
                base, target, null, null, new BigDecimal("0.7500"), 30, false
        );

        mockMvc.perform(post("/api/v1/market-comparison/compare")
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.similarity.isCompatible", is(false)))
                .andExpect(jsonPath("$.similarity.compatibilityStatus", is("INCOMPATIBLE")))
                .andExpect(jsonPath("$.similarity.isEquivalent", is(false))) // NUNCA equivalente
                .andExpect(jsonPath("$.similarity.warningMessage", containsString("AVISO DE INCOMPATIBILIDADE")))
                .andExpect(jsonPath("$.priceComparison.baseShippingStatus", is("UNKNOWN")))
                .andExpect(jsonPath("$.priceComparison.baseFinalPriceWithShipping", nullValue())) // Não inferido
                .andExpect(jsonPath("$.priceComparison.totalPriceDifferenceWithShipping", nullValue()))
                .andExpect(jsonPath("$.priceComparison.shippingNotice", containsString("Custo de frete não informado")));
    }

    @Test
    @DisplayName("POST /api/v1/market-comparison/find-equivalents: Deve ranquear candidatos ordenando por compatibilidade e score")
    void shouldFindAndRankEquivalents() throws Exception {
        ComparableListingItem base = ComparableListingItem.builder()
                .title("Notebook Dell Inspiron 15 i5 8GB 256GB SSD")
                .brand("Dell")
                .model("Inspiron 15")
                .price(new BigDecimal("3000.00"))
                .condition(ItemCondition.NEW)
                .build();

        ComparableListingItem c1 = ComparableListingItem.builder()
                .title("Notebook Dell Inspiron 15 Intel i5 8GB 256GB")
                .brand("Dell")
                .model("Inspiron 15")
                .price(new BigDecimal("2950.00"))
                .condition(ItemCondition.NEW)
                .build();

        ComparableListingItem c2 = ComparableListingItem.builder()
                .title("Notebook Lenovo Ideapad 3 Ryzen 5 8GB")
                .brand("Lenovo") // Incompatível de marca
                .price(new BigDecimal("2700.00"))
                .condition(ItemCondition.NEW)
                .build();

        FindEquivalentsRequest request = new FindEquivalentsRequest(
                base, null, List.of(c2, c1), new BigDecimal("0.7500"), 30
        );

        mockMvc.perform(post("/api/v1/market-comparison/find-equivalents")
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                // c1 (compatível) deve vir antes de c2 (incompatível)
                .andExpect(jsonPath("$[0].targetListing.brand", is("Dell")))
                .andExpect(jsonPath("$[0].similarity.isCompatible", is(true)))
                .andExpect(jsonPath("$[1].targetListing.brand", is("Lenovo")))
                .andExpect(jsonPath("$[1].similarity.isCompatible", is(false)));
    }

    @Test
    @DisplayName("POST /api/v1/market-comparison/price-history: Deve registrar ponto de preço e GET retornar histórico legítimo")
    void shouldRecordAndRetrievePriceHistoryWithoutFakeData() throws Exception {
        RecordPricePointRequest point1 = new RecordPricePointRequest(
                "MERCADO_LIVRE", "MLB-HIST-01", "Headset Gamer HyperX Cloud Stinger",
                new BigDecimal("250.00"), new BigDecimal("15.00"),
                Instant.now().minus(2, ChronoUnit.DAYS), "OFFICIAL", "https://produto.mercadolivre.com.br/MLB-HIST-01"
        );

        mockMvc.perform(post("/api/v1/market-comparison/price-history")
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(point1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.price", is(250.0)))
                .andExpect(jsonPath("$.shippingStatus", is("KNOWN")));

        // Consulta histórico
        mockMvc.perform(get("/api/v1/market-comparison/price-history")
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .param("marketplace", "MERCADO_LIVRE")
                        .param("externalListingId", "MLB-HIST-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataPointsCount", is(1)))
                .andExpect(jsonPath("$.hasHistoricalTrend", is(false)))
                .andExpect(jsonPath("$.historyNotice", containsString("NUNCA inventa dados retroativos passados")))
                .andExpect(jsonPath("$.points", hasSize(1)));
    }

    @Test
    @DisplayName("GET /api/v1/market-comparison/saved: Deve listar comparações salvas com isolamento tenant")
    void shouldListSavedComparisonsWithTenantIsolation() throws Exception {
        mockMvc.perform(get("/api/v1/market-comparison/saved")
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/market-comparison/price-history: Deve rejeitar requisição sem autorização com 401")
    void shouldRejectUnauthorizedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/market-comparison/price-history")
                        .param("marketplace", "MERCADO_LIVRE")
                        .param("externalListingId", "MLB-001"))
                .andExpect(status().isUnauthorized());
    }
}
