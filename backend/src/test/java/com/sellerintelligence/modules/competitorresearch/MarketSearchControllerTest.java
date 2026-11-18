package com.sellerintelligence.modules.competitorresearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellerintelligence.core.security.JwtTokenProvider;
import com.sellerintelligence.modules.catalog.MarketplaceListing;
import com.sellerintelligence.modules.catalog.MarketplaceListingRepository;
import com.sellerintelligence.modules.catalog.SellerAccount;
import com.sellerintelligence.modules.catalog.SellerAccountRepository;
import com.sellerintelligence.modules.competitorresearch.model.MarketSearchQueryRecord;
import com.sellerintelligence.modules.competitorresearch.model.MarketSearchQueryRepository;
import com.sellerintelligence.modules.competitorresearch.service.MarketSearchRateLimiter;
import com.sellerintelligence.modules.marketintelligence.MarketObservation;
import com.sellerintelligence.modules.marketintelligence.MarketObservationRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MarketSearchControllerTest {

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
    private MarketplaceListingRepository listingRepository;

    @Autowired
    private SellerAccountRepository sellerAccountRepository;

    @Autowired
    private MarketSearchQueryRepository queryRepository;

    @Autowired
    private MarketSearchRateLimiter rateLimiter;

    private String tokenAlpha;
    private String tokenBeta;
    private UUID sellerAccountAlphaId;

    @BeforeEach
    void setUp() {
        rateLimiter.resetForTest();
        tokenAlpha = jwtTokenProvider.generateToken(USER_ID, "alpha-admin@sellerintelligence.com", TENANT_ALPHA_ID, List.of("ROLE_ADMIN"));
        tokenBeta = jwtTokenProvider.generateToken(USER_ID, "beta-admin@sellerintelligence.com", TENANT_BETA_ID, List.of("ROLE_ADMIN"));

        sellerAccountAlphaId = UUID.randomUUID();
        SellerAccount account = new SellerAccount(
                sellerAccountAlphaId,
                TENANT_ALPHA_ID,
                "SHOPEE",
                "SHOP-BR-ALPHA",
                "Loja Alpha Oficial",
                "ACTIVE"
        );
        sellerAccountRepository.save(account);
    }

    @Test
    @DisplayName("GET /api/v1/market-search: Deve realizar busca autorizada no Mercado Livre com sucesso e rastreabilidade")
    void shouldExecuteAuthorizedMlbSearchSuccessfully() throws Exception {
        // Observação no tenant Alpha
        MarketObservation obs = new MarketObservation(
                UUID.randomUUID(),
                TENANT_ALPHA_ID,
                "MLB-99001122",
                "MERCADO_LIVRE",
                "Cadeira Gamer Ergonômica Pro",
                new BigDecimal("899.90"),
                "BRL",
                ItemCondition.NEW,
                "Loja dos Gamers Oficial",
                MarketObservationSourceType.OBSERVED,
                "https://produto.mercadolivre.com.br/MLB-99001122",
                Instant.now(),
                Instant.now(),
                Instant.now().plus(7, ChronoUnit.DAYS),
                "PUBLIC_API_OBSERVATION",
                new BigDecimal("1.0000"),
                null, null, null, "7891112223334", "GamerPro", null, null, null, null, "{}"
        );
        observationRepository.save(obs);

        mockMvc.perform(get("/api/v1/market-search")
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .param("q", "Cadeira Gamer")
                        .param("marketplace", "MERCADO_LIVRE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capabilityStatus").value("SUPPORTED"))
                .andExpect(jsonPath("$.sourcePolicyId").value("POL-MLB-PUBLIC-01"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].itemId").value("MLB-99001122"))
                .andExpect(jsonPath("$.items[0].title").value("Cadeira Gamer Ergonômica Pro"))
                .andExpect(jsonPath("$.items[0].sourcePolicyId").value("POL-MLB-PUBLIC-01"))
                .andExpect(jsonPath("$.items[0].provenanceType").value("OBSERVED"))
                .andExpect(jsonPath("$.items[0].salesMetricNotice", containsString("não foram fabricadas")));
    }

    @Test
    @DisplayName("GET /api/v1/market-search: Amazon deve retornar status UNVERIFIED via política Fail-Closed sem dados fabricados")
    void shouldReturnUnverifiedForAmazonSearchFailClosed() throws Exception {
        mockMvc.perform(get("/api/v1/market-search")
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .param("q", "Alexa Echo Dot")
                        .param("marketplace", "AMAZON")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capabilityStatus").value("UNVERIFIED"))
                .andExpect(jsonPath("$.sourcePolicyId").value("POL-AMAZON-UNVERIFIED-01"))
                .andExpect(jsonPath("$.items", hasSize(0)))
                .andExpect(jsonPath("$.capabilityNotice", containsString("Fail-Closed")));
    }

    @Test
    @DisplayName("GET /api/v1/market-search: Shopee deve retornar RESTRICTED, aviso anti-scraping e fallback de catálogo próprio")
    void shouldReturnRestrictedForShopeeWithOwnCatalogFallback() throws Exception {
        MarketplaceListing ownListing = new MarketplaceListing(
                UUID.randomUUID(),
                TENANT_ALPHA_ID,
                sellerAccountAlphaId,
                null,
                null,
                "SHOPEE",
                "SHOPEE-5544",
                "SKU-GAR-01",
                null,
                "Garrafa Térmica Inox Própria",
                new BigDecimal("79.90"),
                "BRL",
                50,
                "ACTIVE",
                "https://shopee.com.br/product/own/5544",
                "{}",
                "v1",
                "TEST"
        );
        listingRepository.save(ownListing);

        mockMvc.perform(get("/api/v1/market-search")
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .param("q", "Garrafa Térmica")
                        .param("marketplace", "SHOPEE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capabilityStatus").value("RESTRICTED"))
                .andExpect(jsonPath("$.capabilityNotice", containsString("não realiza web scraping")))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].itemId").value("SHOPEE-5544"))
                .andExpect(jsonPath("$.items[0].provenanceType").value("OFFICIAL"))
                .andExpect(jsonPath("$.items[0].sellerClassification").value("Próprio"));
    }

    @Test
    @DisplayName("GET /api/v1/market-search/sources: Deve listar as 5 políticas oficiais de integração com documentação e limites")
    void shouldListRegisteredSourcePolicies() throws Exception {
        mockMvc.perform(get("/api/v1/market-search/sources")
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(5))))
                .andExpect(jsonPath("$[*].id", hasItems("POL-MLB-PUBLIC-01", "POL-SHOPEE-RESTRICTED-01", "POL-AMAZON-UNVERIFIED-01", "POL-OWN-CATALOG-01")));
    }

    @Test
    @DisplayName("GET /api/v1/market-search/history: Deve respeitar isolamento estrito entre Tenant Alpha e Tenant Beta")
    void shouldIsolateSearchHistoryBetweenTenants() throws Exception {
        queryRepository.save(new MarketSearchQueryRecord(
                UUID.randomUUID(),
                TENANT_ALPHA_ID,
                "Monitor LG",
                "MERCADO_LIVRE",
                null, null, null, false,
                "POL-MLB-PUBLIC-01",
                "SUPPORTED",
                5, 120, false, Instant.now()
        ));

        queryRepository.save(new MarketSearchQueryRecord(
                UUID.randomUUID(),
                TENANT_BETA_ID,
                "Notebook Dell Secreto Beta",
                "MERCADO_LIVRE",
                null, null, null, false,
                "POL-MLB-PUBLIC-01",
                "SUPPORTED",
                2, 80, false, Instant.now()
        ));

        // Consulta pelo Tenant Alpha: deve ver apenas "Monitor LG"
        mockMvc.perform(get("/api/v1/market-search/history")
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].queryText", hasItem("Monitor LG")))
                .andExpect(jsonPath("$.content[*].queryText", not(hasItem("Notebook Dell Secreto Beta"))));
    }

    @Test
    @DisplayName("Rate Limit: Requisições acima do limite devem retornar HTTP 429 com Retry-After")
    void shouldReturn429WhenRateLimitIsExceeded() throws Exception {
        // Esgota a cota da política Shopee (10 req/min)
        for (int i = 0; i < 10; i++) {
            rateLimiter.checkAndConsume(TENANT_ALPHA_ID, "POL-SHOPEE-RESTRICTED-01", 10);
        }

        // A 11ª chamada pelo serviço deve disparar HTTP 429
        mockMvc.perform(get("/api/v1/market-search")
                        .header("Authorization", "Bearer " + tokenAlpha)
                        .param("q", "Item Extra Inédito")
                        .param("marketplace", "SHOPEE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.title").value("Limite de Taxa Excedido"));
    }
}
