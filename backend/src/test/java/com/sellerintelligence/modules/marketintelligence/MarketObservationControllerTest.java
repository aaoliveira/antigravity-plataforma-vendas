package com.sellerintelligence.modules.marketintelligence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellerintelligence.core.security.JwtTokenProvider;
import com.sellerintelligence.modules.catalog.Product;
import com.sellerintelligence.modules.catalog.ProductRepository;
import com.sellerintelligence.modules.catalog.ProductVariant;
import com.sellerintelligence.modules.catalog.ProductVariantRepository;
import com.sellerintelligence.modules.marketintelligence.model.CreateObservationRequest;
import com.sellerintelligence.modules.marketintelligence.model.ExecuteMatchRequest;
import com.sellerintelligence.modules.marketintelligence.model.ItemCondition;
import com.sellerintelligence.modules.marketintelligence.model.MarketObservationSourceType;
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
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MarketObservationControllerTest {

    private static final UUID TENANT_ALPHA_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TENANT_BETA_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID USER_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private MarketObservationRepository observationRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private ProductMatchRepository productMatchRepository;

    private String tokenAlpha;
    private String tokenBeta;
    private MarketObservation observationAlpha;
    private Product productAlpha;

    @BeforeEach
    void setUp() {
        tokenAlpha = jwtTokenProvider.generateToken(USER_ID, "alpha@example.com", TENANT_ALPHA_ID, List.of("ADMIN"));
        tokenBeta = jwtTokenProvider.generateToken(USER_ID, "beta@example.com", TENANT_BETA_ID, List.of("ADMIN"));

        observationAlpha = new MarketObservation(
                UUID.randomUUID(),
                TENANT_ALPHA_ID,
                "MLB-OBS-100",
                "MERCADO_LIVRE",
                "Monitor Gamer 24 Polegadas 144Hz Full HD",
                new BigDecimal("899.9000"),
                "BRL",
                ItemCondition.NEW,
                "Loja Gamer Oficial",
                MarketObservationSourceType.OBSERVED,
                "https://produto.mercadolivre.com.br/MLB-OBS-100",
                Instant.now(),
                Instant.now(),
                Instant.now().plus(7, ChronoUnit.DAYS),
                "PUBLIC_API_OBSERVATION",
                BigDecimal.ONE,
                null,
                null,
                null,
                "7899999999999",
                "AOC",
                null,
                null,
                null,
                null,
                "{\"brand\":\"AOC\",\"screen_size\":\"24\"}"
        );
        observationRepository.save(observationAlpha);

        productAlpha = new Product(
                UUID.randomUUID(),
                TENANT_ALPHA_ID,
                "Monitor Gamer AOC 24 144Hz",
                "SKU-AOC-24",
                "AOC",
                "Monitores",
                "ACTIVE"
        );
        productRepository.save(productAlpha);

        ProductVariant variantAlpha = new ProductVariant(
                UUID.randomUUID(),
                TENANT_ALPHA_ID,
                productAlpha.getId(),
                "SKU-AOC-24-VAR",
                "7899999999999",
                "{}",
                "ACTIVE"
        );
        productVariantRepository.save(variantAlpha);
    }

    @Test
    @DisplayName("POST /api/v1/market-intelligence/observations: Deve criar observação válida com proveniência e retornar 201")
    void shouldCreateObservationSuccessfully() throws Exception {
        CreateObservationRequest request = new CreateObservationRequest(
                "MLB-NEW-200",
                "MERCADO_LIVRE",
                "Teclado Mecânico RGB Switch Blue",
                new BigDecimal("199.9000"),
                "BRL",
                ItemCondition.NEW,
                "Loja dos Teclados",
                MarketObservationSourceType.OBSERVED,
                "https://produto.mercadolivre.com.br/MLB-NEW-200",
                Instant.now(),
                Instant.now(),
                Instant.now().plus(7, ChronoUnit.DAYS),
                "SCRAPER_V1",
                BigDecimal.ONE,
                null,
                null,
                null,
                "7898888888888",
                "Redragon",
                null,
                null,
                null,
                null,
                "{\"switch\":\"blue\"}"
        );

        mockMvc.perform(post("/api/v1/market-intelligence/observations")
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.observedListingId", is("MLB-NEW-200")))
                .andExpect(jsonPath("$.sourceType", is("OBSERVED")))
                .andExpect(jsonPath("$.confidence").value(1))
                .andExpect(jsonPath("$.gtin", is("7898888888888")));
    }

    @Test
    @DisplayName("POST /api/v1/market-intelligence/observations: Deve retornar 422 quando regra anti-alucinação é violada")
    void shouldRejectObservationViolatingAntiHallucinationRules() throws Exception {
        CreateObservationRequest invalidRequest = new CreateObservationRequest(
                "MLB-INVALID",
                "MERCADO_LIVRE",
                "Item Alucinado",
                new BigDecimal("50.0000"),
                "BRL",
                ItemCondition.NEW,
                null,
                MarketObservationSourceType.OBSERVED,
                "https://produto.mercadolivre.com.br/MLB-INVALID",
                Instant.now(),
                Instant.now(),
                Instant.now().plus(7, ChronoUnit.DAYS),
                "ESTIMATE",
                BigDecimal.ONE,
                null, // metodologia ausente
                null, // modelVersion ausente
                null,
                null,
                null,
                new BigDecimal("1000.0000"), // salesVolume fornecido sem metodologia!
                null,
                null,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/market-intelligence/observations")
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title", is("Violação de Regra de Negócio")))
                .andExpect(jsonPath("$.detail", containsString("Valores de vendas, visitas, CTR ou faturamento são proibidos sem fonte verificável e metodologia documentada.")));
    }

    @Test
    @DisplayName("GET /api/v1/market-intelligence/observations: Deve listar com filtros e paginação")
    void shouldListObservationsWithPaginationAndFilters() throws Exception {
        mockMvc.perform(get("/api/v1/market-intelligence/observations")
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .param("marketplace", "MERCADO_LIVRE")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.items[0].observedListingId", is("MLB-OBS-100")))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("GET /api/v1/market-intelligence/observations/{id}: Bloqueio cross-tenant retorna 404")
    void shouldBlockCrossTenantObservationAccess() throws Exception {
        // Tenant Beta tenta acessar observação do Tenant Alpha
        mockMvc.perform(get("/api/v1/market-intelligence/observations/{id}", observationAlpha.getId())
                        .header("Authorization", "Bearer " + tokenBeta))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/market-intelligence/observations/{id}/match: Deve executar matching e retornar candidatos explicáveis")
    void shouldExecuteMatchingAndReturnCandidates() throws Exception {
        ExecuteMatchRequest matchRequest = new ExecuteMatchRequest(new BigDecimal("0.7500"));

        mockMvc.perform(post("/api/v1/market-intelligence/observations/{id}/match", observationAlpha.getId())
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(matchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].canonicalProductId", is(productAlpha.getId().toString())))
                .andExpect(jsonPath("$[0].scoreBreakdown.status", is("MATCHED")))
                .andExpect(jsonPath("$[0].scoreBreakdown.explanation", containsString("GTIN idêntico")));
    }

    @Test
    @DisplayName("GET /api/v1/market-intelligence/matches: Deve listar matches salvos")
    void shouldListMatchesSuccessfully() throws Exception {
        // Primeiro executa o match para persistir
        mockMvc.perform(post("/api/v1/market-intelligence/observations/{id}/match", observationAlpha.getId())
                        .header("Authorization", "Bearer " + tokenAlpha))
                .andExpect(status().isOk());

        // Depois consulta listagem
        mockMvc.perform(get("/api/v1/market-intelligence/matches")
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .param("status", "MATCHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.items[0].matchStatus", is("MATCHED")));
    }
}
