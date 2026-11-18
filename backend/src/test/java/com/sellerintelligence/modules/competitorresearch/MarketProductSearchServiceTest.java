package com.sellerintelligence.modules.competitorresearch;

import com.sellerintelligence.modules.catalog.MarketplaceListing;
import com.sellerintelligence.modules.catalog.MarketplaceListingRepository;
import com.sellerintelligence.modules.competitorresearch.model.MarketSearchQueryRecord;
import com.sellerintelligence.modules.competitorresearch.model.MarketSearchQueryRepository;
import com.sellerintelligence.modules.competitorresearch.model.MarketSearchRequest;
import com.sellerintelligence.modules.competitorresearch.model.MarketSearchResponse;
import com.sellerintelligence.modules.competitorresearch.model.MarketSearchSourcePolicy;
import com.sellerintelligence.modules.competitorresearch.model.NormalizedMarketSearchResult;
import com.sellerintelligence.modules.competitorresearch.service.MarketProductSearchService;
import com.sellerintelligence.modules.competitorresearch.service.MarketSearchCacheService;
import com.sellerintelligence.modules.competitorresearch.service.MarketSearchRateLimiter;
import com.sellerintelligence.modules.competitorresearch.service.SearchSourcePolicyRegistry;
import com.sellerintelligence.modules.marketintelligence.MarketObservation;
import com.sellerintelligence.modules.marketintelligence.MarketObservationRepository;
import com.sellerintelligence.modules.marketintelligence.model.ItemCondition;
import com.sellerintelligence.modules.marketintelligence.model.MarketObservationSourceType;
import com.sellerintelligence.modules.marketplaceadapters.model.CapabilityStatus;
import com.sellerintelligence.modules.marketplaceadapters.model.MarketplaceCapability;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketProductSearchServiceTest {

    @Mock
    private SearchSourcePolicyRegistry policyRegistry;

    @Mock
    private MarketSearchCacheService cacheService;

    @Mock
    private MarketSearchRateLimiter rateLimiter;

    @Mock
    private MarketSearchQueryRepository queryRepository;

    @Mock
    private MarketplaceListingRepository listingRepository;

    @Mock
    private MarketObservationRepository observationRepository;

    private MarketProductSearchService searchService;

    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        searchService = new MarketProductSearchService(
                policyRegistry,
                cacheService,
                rateLimiter,
                queryRepository,
                listingRepository,
                observationRepository
        );
    }

    @Test
    @DisplayName("Fail-Closed: Canal Amazon UNVERIFIED deve retornar status UNVERIFIED sem chamar APIs e com zero itens")
    void shouldBlockUnverifiedAmazonSearchFailClosed() {
        MarketSearchRequest request = new MarketSearchRequest("fone bluetooth", "AMAZON", null, null, null, false, 0, 20);
        when(policyRegistry.resolvePolicyForSearch("AMAZON", false)).thenReturn(createAmazonPolicy());

        MarketSearchResponse response = searchService.search(tenantId, request);

        assertThat(response).isNotNull();
        assertThat(response.capabilityStatus()).isEqualTo(CapabilityStatus.UNVERIFIED);
        assertThat(response.items()).isEmpty();
        assertThat(response.capabilityNotice()).contains("UNVERIFIED");
        assertThat(response.capabilityNotice()).contains("Fail-Closed");

        verifyNoInteractions(observationRepository);
        verify(queryRepository).save(any(MarketSearchQueryRecord.class));
    }

    @Test
    @DisplayName("Anti-Scraping: Shopee RESTRICTED não realiza scraping e faz fallback para catálogo próprio")
    void shouldEnforceAntiScrapingOnShopeeAndFallbackToOwnCatalog() {
        MarketSearchRequest request = new MarketSearchRequest("teclado mecanico", "SHOPEE", null, null, null, false, 0, 20);
        when(policyRegistry.resolvePolicyForSearch("SHOPEE", false)).thenReturn(createShopeePolicy());

        MarketplaceListing ownListing = new MarketplaceListing(
                UUID.randomUUID(),
                tenantId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                "SHOPEE",
                "SHOPEE-9988",
                "SKU-TEC-01",
                null,
                "Teclado Mecânico RGB Próprio",
                new BigDecimal("299.90"),
                "BRL",
                15,
                "ACTIVE",
                "https://shopee.com.br/product/own/9988",
                "{}",
                "v1",
                "TEST"
        );

        when(listingRepository.searchListings(eq(tenantId), eq("SHOPEE"), any(), any(), eq("teclado mecanico"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ownListing)));
        when(rateLimiter.checkAndConsume(eq(tenantId), eq(SearchSourcePolicyRegistry.POLICY_SHOPEE_RESTRICTED), anyInt()))
                .thenReturn(9);

        MarketSearchResponse response = searchService.search(tenantId, request);

        assertThat(response).isNotNull();
        assertThat(response.capabilityStatus()).isEqualTo(CapabilityStatus.RESTRICTED);
        assertThat(response.capabilityNotice()).contains("não realiza web scraping");
        assertThat(response.items()).hasSize(1);

        NormalizedMarketSearchResult item = response.items().get(0);
        assertThat(item.itemId()).isEqualTo("SHOPEE-9988");
        assertThat(item.title()).isEqualTo("Teclado Mecânico RGB Próprio");
        assertThat(item.provenanceType()).isEqualTo("OFFICIAL");
        assertThat(item.sellerClassification()).isEqualTo("Próprio");

        verifyNoInteractions(observationRepository);
    }

    @Test
    @DisplayName("Mercado Livre SUPPORTED: Executa pesquisa com rastreabilidade e aviso estrito contra dados inventados")
    void shouldExecuteMlbSearchWithTraceabilityAndNoFabricatedSales() {
        MarketSearchRequest request = new MarketSearchRequest("monitor dell", "MERCADO_LIVRE", null, null, null, false, 0, 20);
        when(policyRegistry.resolvePolicyForSearch("MERCADO_LIVRE", false)).thenReturn(createMlbPolicy());
        when(cacheService.computeCacheKey(tenantId, request)).thenReturn("key-mlb-123");
        when(cacheService.getValidCache(tenantId, "key-mlb-123")).thenReturn(Optional.empty());
        when(rateLimiter.checkAndConsume(eq(tenantId), eq(SearchSourcePolicyRegistry.POLICY_MLB_PUBLIC), anyInt()))
                .thenReturn(59);

        MarketObservation obs = new MarketObservation(
                UUID.randomUUID(),
                tenantId,
                "MLB-99112233",
                "MERCADO_LIVRE",
                "Monitor Dell 27 Polegadas 4K",
                new BigDecimal("2199.00"),
                "BRL",
                ItemCondition.NEW,
                "Dell Loja Oficial",
                MarketObservationSourceType.OBSERVED,
                "https://produto.mercadolivre.com.br/MLB-99112233",
                Instant.now(),
                Instant.now(),
                Instant.now().plus(7, ChronoUnit.DAYS),
                "PUBLIC_API_OBSERVATION",
                new BigDecimal("1.0000"),
                null,
                null,
                null,
                "7891234567890",
                "Dell",
                null, // Proibido fabricar vendas
                null,
                null,
                null,
                "{}"
        );

        when(observationRepository.findWithFilters(eq(tenantId), eq("MERCADO_LIVRE"), any(), any(), any(), any(), eq("monitor dell"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(obs)));

        MarketSearchResponse response = searchService.search(tenantId, request);

        assertThat(response).isNotNull();
        assertThat(response.capabilityStatus()).isEqualTo(CapabilityStatus.SUPPORTED);
        assertThat(response.cached()).isFalse();
        assertThat(response.items()).hasSize(1);

        NormalizedMarketSearchResult item = response.items().get(0);
        assertThat(item.itemId()).isEqualTo("MLB-99112233");
        assertThat(item.sourcePolicyId()).isEqualTo(SearchSourcePolicyRegistry.POLICY_MLB_PUBLIC);
        assertThat(item.provenanceType()).isEqualTo("OBSERVED");
        assertThat(item.salesMetricNotice()).contains("UNSUPPORTED");
        assertThat(item.salesMetricNotice()).contains("não foram fabricadas");

        verify(cacheService).putCache(eq(tenantId), eq("key-mlb-123"), eq(SearchSourcePolicyRegistry.POLICY_MLB_PUBLIC), any(), any());
        verify(queryRepository).save(any(MarketSearchQueryRecord.class));
    }

    @Test
    @DisplayName("Cache Hit: Retorna resposta em cache com flag cached=true sem consumir quota de rate limit")
    void shouldReturnCachedResponseWithoutConsumingQuotaWhenCacheIsValid() {
        MarketSearchRequest request = new MarketSearchRequest("mouse sem fio", "MERCADO_LIVRE", null, null, null, false, 0, 20);
        when(policyRegistry.resolvePolicyForSearch("MERCADO_LIVRE", false)).thenReturn(createMlbPolicy());
        when(cacheService.computeCacheKey(tenantId, request)).thenReturn("key-mouse-cache");

        MarketSearchResponse cachedResponse = new MarketSearchResponse(
                List.of(new NormalizedMarketSearchResult(
                        "MLB-1122", "Mouse Sem Fio Logitech", "MERCADO_LIVRE", new BigDecimal("120.00"),
                        "BRL", "Logitech", "Oficial", "https://produto.mercadolivre.com.br/MLB-1122",
                        null, "NEW", null, SearchSourcePolicyRegistry.POLICY_MLB_PUBLIC, "OBSERVED",
                        Instant.now(), "Notice"
                )),
                1, 1, 0, 20, "MERCADO_LIVRE", SearchSourcePolicyRegistry.POLICY_MLB_PUBLIC,
                CapabilityStatus.SUPPORTED, true, "Do Cache", 60
        );

        when(cacheService.getValidCache(tenantId, "key-mouse-cache")).thenReturn(Optional.of(cachedResponse));

        MarketSearchResponse response = searchService.search(tenantId, request);

        assertThat(response).isNotNull();
        assertThat(response.cached()).isTrue();
        assertThat(response.items()).hasSize(1);

        verifyNoInteractions(observationRepository);
        verify(rateLimiter, never()).checkAndConsume(any(), any(), anyInt());
        verify(cacheService, never()).putCache(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Filtros de Preço: Itens fora da faixa minPrice e maxPrice são filtrados")
    void shouldFilterByMinAndMaxPrice() {
        MarketSearchRequest request = new MarketSearchRequest("fone", "MERCADO_LIVRE", null,
                new BigDecimal("100.00"), new BigDecimal("300.00"), false, 0, 20);
        when(policyRegistry.resolvePolicyForSearch("MERCADO_LIVRE", false)).thenReturn(createMlbPolicy());
        when(cacheService.computeCacheKey(tenantId, request)).thenReturn("key-fone-range");
        when(cacheService.getValidCache(tenantId, "key-fone-range")).thenReturn(Optional.empty());

        MarketObservation cheap = createTestObservation(tenantId, "MLB-CHEAP", new BigDecimal("50.00"));
        MarketObservation mid = createTestObservation(tenantId, "MLB-MID", new BigDecimal("250.00"));
        MarketObservation expensive = createTestObservation(tenantId, "MLB-EXP", new BigDecimal("990.00"));

        when(observationRepository.findWithFilters(eq(tenantId), eq("MERCADO_LIVRE"), any(), any(), any(), any(), eq("fone"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(cheap, mid, expensive)));

        MarketSearchResponse response = searchService.search(tenantId, request);

        assertThat(response).isNotNull();
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).itemId()).isEqualTo("MLB-MID");
        assertThat(response.items().get(0).price()).isEqualByComparingTo("250.00");
    }

    private MarketSearchSourcePolicy createAmazonPolicy() {
        return new MarketSearchSourcePolicy(
                SearchSourcePolicyRegistry.POLICY_AMAZON_UNVERIFIED,
                "Amazon SP-API Pricing & Catalog",
                "AMAZON",
                MarketplaceCapability.MARKET_SEARCH,
                CapabilityStatus.UNVERIFIED,
                "LWA_IAM_CREDENTIALS",
                "https://sellingpartnerapi-na.amazon.com/catalog/2022-04-01/items",
                "https://developer-docs.amazon.com/sp-api",
                0,
                false,
                "Regras Amazon",
                true
        );
    }

    private MarketSearchSourcePolicy createShopeePolicy() {
        return new MarketSearchSourcePolicy(
                SearchSourcePolicyRegistry.POLICY_SHOPEE_RESTRICTED,
                "Shopee Open Platform Product Search",
                "SHOPEE",
                MarketplaceCapability.MARKET_SEARCH,
                CapabilityStatus.RESTRICTED,
                "HMAC_PARTNER_APP",
                "https://partner.shopeemobile.com/api/v2/product/search_item",
                "https://open.shopee.com/documents/v2/v2.product.search_item",
                10,
                false,
                "Regras Shopee",
                true
        );
    }

    private MarketSearchSourcePolicy createMlbPolicy() {
        return new MarketSearchSourcePolicy(
                SearchSourcePolicyRegistry.POLICY_MLB_PUBLIC,
                "Mercado Livre Pesquisa Pública e Catálogo",
                "MERCADO_LIVRE",
                MarketplaceCapability.MARKET_SEARCH,
                CapabilityStatus.SUPPORTED,
                "PUBLIC_RATE_LIMITED",
                "https://api.mercadolibre.com/sites/MLB/search",
                "https://developers.mercadolibre.com.br/pt_br/pesquisa-de-produtos",
                60,
                true,
                "Regras MLB",
                true
        );
    }

    private MarketObservation createTestObservation(UUID tenantId, String id, BigDecimal price) {
        return new MarketObservation(
                UUID.randomUUID(),
                tenantId,
                id,
                "MERCADO_LIVRE",
                "Produto " + id,
                price,
                "BRL",
                ItemCondition.NEW,
                "Vendedor Teste",
                MarketObservationSourceType.OBSERVED,
                "https://produto.mercadolivre.com.br/" + id,
                Instant.now(),
                Instant.now(),
                Instant.now().plus(7, ChronoUnit.DAYS),
                "API_TEST",
                new BigDecimal("1.0000"),
                null, null, null, null, null, null, null, null, null, "{}"
        );
    }
}
