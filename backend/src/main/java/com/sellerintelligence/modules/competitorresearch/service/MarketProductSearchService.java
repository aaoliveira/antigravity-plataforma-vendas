package com.sellerintelligence.modules.competitorresearch.service;

import com.sellerintelligence.modules.catalog.MarketplaceListing;
import com.sellerintelligence.modules.catalog.MarketplaceListingRepository;
import com.sellerintelligence.modules.competitorresearch.model.MarketSearchHistoryItemDto;
import com.sellerintelligence.modules.competitorresearch.model.MarketSearchQueryRecord;
import com.sellerintelligence.modules.competitorresearch.model.MarketSearchQueryRepository;
import com.sellerintelligence.modules.competitorresearch.model.MarketSearchRequest;
import com.sellerintelligence.modules.competitorresearch.model.MarketSearchResponse;
import com.sellerintelligence.modules.competitorresearch.model.MarketSearchSourcePolicy;
import com.sellerintelligence.modules.competitorresearch.model.MarketSearchSourcePolicyDto;
import com.sellerintelligence.modules.competitorresearch.model.NormalizedMarketSearchResult;
import com.sellerintelligence.modules.competitorresearch.model.PageResponse;
import com.sellerintelligence.modules.marketintelligence.MarketObservation;
import com.sellerintelligence.modules.marketintelligence.MarketObservationRepository;
import com.sellerintelligence.modules.marketplaceadapters.model.CapabilityStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço central de pesquisa de produtos e anúncios em conformidade com as políticas oficiais dos marketplaces.
 * Garante que:
 * 1. Nenhuma fonte não aprovada seja chamada (Fail-Closed).
 * 2. Scraping hostil jamais seja utilizado quando um canal for RESTRICTED ou UNSUPPORTED.
 * 3. Métricas de vendas unitárias ou faturamento de terceiros nunca sejam forjadas ou inventadas.
 * 4. Respeito rigoroso aos rate limits e caching inteligente com TTL.
 */
@Service
public class MarketProductSearchService {

    private static final Logger log = LoggerFactory.getLogger(MarketProductSearchService.class);

    private static final String NOTICE_UNSUPPORTED_SALES_METRICS =
            "Métricas de vendas unitárias e faturamento de terceiros não são fornecidas pela API oficial do canal (UNSUPPORTED) e não foram fabricadas.";

    private final SearchSourcePolicyRegistry policyRegistry;
    private final MarketSearchCacheService cacheService;
    private final MarketSearchRateLimiter rateLimiter;
    private final MarketSearchQueryRepository queryRepository;
    private final MarketplaceListingRepository listingRepository;
    private final MarketObservationRepository observationRepository;

    public MarketProductSearchService(
            SearchSourcePolicyRegistry policyRegistry,
            MarketSearchCacheService cacheService,
            MarketSearchRateLimiter rateLimiter,
            MarketSearchQueryRepository queryRepository,
            MarketplaceListingRepository listingRepository,
            MarketObservationRepository observationRepository
    ) {
        this.policyRegistry = policyRegistry;
        this.cacheService = cacheService;
        this.rateLimiter = rateLimiter;
        this.queryRepository = queryRepository;
        this.listingRepository = listingRepository;
        this.observationRepository = observationRepository;
    }

    @Transactional
    public MarketSearchResponse search(UUID tenantId, MarketSearchRequest request) {
        long startTime = System.currentTimeMillis();
        boolean onlyOwn = Boolean.TRUE.equals(request.onlyOwnCatalog());

        MarketSearchSourcePolicy policy = policyRegistry.resolvePolicyForSearch(request.marketplace(), onlyOwn);

        // 1. Verificação Fail-Closed para fontes UNVERIFIED (ex: Amazon SP-API sem homologação)
        if (policy.getCapabilityStatus() == CapabilityStatus.UNVERIFIED) {
            String notice = String.format(
                    "Pesquisa no canal '%s' não homologada ou não verificada documentalmente (UNVERIFIED). " +
                    "Conforme a política Fail-Closed, chamadas externas estão bloqueadas para proteção da conta.",
                    policy.getMarketplace()
            );
            recordAuditQuery(tenantId, request, policy.getId(), CapabilityStatus.UNVERIFIED.name(), 0, startTime, false);
            return MarketSearchResponse.empty(
                    policy.getMarketplace(),
                    policy.getId(),
                    CapabilityStatus.UNVERIFIED,
                    notice,
                    0
            );
        }

        // 2. Verificação de Cache com Validade (TTL de 15 minutos)
        String cacheKey = cacheService.computeCacheKey(tenantId, request);
        Optional<MarketSearchResponse> cachedResponse = cacheService.getValidCache(tenantId, cacheKey);
        if (cachedResponse.isPresent()) {
            recordAuditQuery(tenantId, request, policy.getId(), policy.getCapabilityStatus().name(),
                    cachedResponse.get().items().size(), startTime, true);
            return cachedResponse.get();
        }

        // 3. Verificação de Rate Limit oficial
        int remainingQuota = rateLimiter.checkAndConsume(tenantId, policy.getId(), policy.getRateLimitPerMinute());

        // 4. Proteção Anti-Scraping para fontes RESTRICTED (ex: Shopee sem busca concorrencial aberta)
        if (policy.getCapabilityStatus() == CapabilityStatus.RESTRICTED && !policy.isAllowsCompetitorSearch() && !onlyOwn) {
            String notice = String.format(
                    "Pesquisa concorrencial aberta não é disponibilizada via API pública da %s (RESTRICTED). " +
                    "Para evitar penalizações na conta do seller e infração dos Termos de Serviço, a plataforma não realiza web scraping. " +
                    "Oferecendo dados correspondentes do próprio catálogo do tenant.",
                    policy.getMarketplace()
            );

            // Fallback seguro: pesquisa anúncios próprios do tenant no marketplace correspondente
            Pageable pageable = PageRequest.of(request.page(), request.size(), Sort.by(Sort.Direction.DESC, "syncedAt"));
            Page<MarketplaceListing> ownListings = listingRepository.searchListings(
                    tenantId,
                    policy.getMarketplace(),
                    null,
                    null,
                    request.q(),
                    pageable
            );

            List<NormalizedMarketSearchResult> items = ownListings.getContent().stream()
                    .filter(l -> isPriceInRange(l.getPrice(), request.minPrice(), request.maxPrice()))
                    .map(l -> mapOwnListingToResult(l, policy.getId()))
                    .toList();

            recordAuditQuery(tenantId, request, policy.getId(), CapabilityStatus.RESTRICTED.name(), items.size(), startTime, false);

            MarketSearchResponse response = new MarketSearchResponse(
                    items,
                    ownListings.getTotalElements(),
                    ownListings.getTotalPages(),
                    request.page(),
                    request.size(),
                    policy.getMarketplace() + " (OWN_CATALOG_FALLBACK)",
                    policy.getId(),
                    CapabilityStatus.RESTRICTED,
                    false,
                    notice,
                    remainingQuota
            );

            cacheService.putCache(tenantId, cacheKey, policy.getId(), response, Duration.ofMinutes(15));
            return response;
        }

        // 5. Execução da Busca Autorizada
        List<NormalizedMarketSearchResult> items = new ArrayList<>();
        long totalElements;
        int totalPages;
        String capabilityNotice;

        if (policy.getId().equals(SearchSourcePolicyRegistry.POLICY_OWN_CATALOG) || onlyOwn) {
            Page<MarketplaceListing> ownPage = listingRepository.searchListings(
                    tenantId,
                    request.marketplace(),
                    null,
                    null,
                    request.q(),
                    PageRequest.of(request.page(), request.size(), Sort.by(Sort.Direction.DESC, "syncedAt"))
            );
            items = ownPage.getContent().stream()
                    .filter(l -> isPriceInRange(l.getPrice(), request.minPrice(), request.maxPrice()))
                    .map(l -> mapOwnListingToResult(l, policy.getId()))
                    .toList();
            totalElements = ownPage.getTotalElements();
            totalPages = ownPage.getTotalPages();
            capabilityNotice = "Pesquisa executada no catálogo próprio do tenant em conformidade com as permissões de acesso.";
        } else {
            // Busca autorizada em observações de mercado (ex: Mercado Livre público homologado)
            Page<MarketObservation> obsPage = observationRepository.findWithFilters(
                    tenantId,
                    policy.getMarketplace(),
                    null,
                    null,
                    null,
                    null,
                    request.q(),
                    PageRequest.of(request.page(), request.size(), Sort.by(Sort.Direction.DESC, "collectedAt"))
            );

            items = obsPage.getContent().stream()
                    .filter(o -> isPriceInRange(o.getPrice(), request.minPrice(), request.maxPrice()))
                    .map(o -> mapObservationToResult(o, policy.getId()))
                    .toList();

            totalElements = obsPage.getTotalElements();
            totalPages = obsPage.getTotalPages();
            capabilityNotice = "Pesquisa pública autorizada realizada com sucesso. Dados concorrenciais rastreáveis e sem fabricação de métricas.";
        }

        MarketSearchResponse response = new MarketSearchResponse(
                items,
                totalElements,
                totalPages,
                request.page(),
                request.size(),
                policy.getMarketplace(),
                policy.getId(),
                policy.getCapabilityStatus(),
                false,
                capabilityNotice,
                remainingQuota
        );

        // 6. Gravação no Cache com validade de 15 minutos
        cacheService.putCache(tenantId, cacheKey, policy.getId(), response, Duration.ofMinutes(15));

        // 7. Auditoria da Consulta
        recordAuditQuery(tenantId, request, policy.getId(), policy.getCapabilityStatus().name(),
                items.size(), startTime, false);

        return response;
    }

    @Transactional(readOnly = true)
    public List<MarketSearchSourcePolicyDto> getPolicies() {
        return policyRegistry.getAllActivePolicies().stream()
                .map(MarketSearchSourcePolicyDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<MarketSearchHistoryItemDto> getSearchHistory(UUID tenantId, Pageable pageable) {
        Page<MarketSearchQueryRecord> page = queryRepository.findAllByTenantId(tenantId, pageable);
        return PageResponse.of(page.map(MarketSearchHistoryItemDto::fromEntity));
    }

    private boolean isPriceInRange(BigDecimal price, BigDecimal minPrice, BigDecimal maxPrice) {
        if (price == null) {
            return true;
        }
        if (minPrice != null && price.compareTo(minPrice) < 0) {
            return false;
        }
        if (maxPrice != null && price.compareTo(maxPrice) > 0) {
            return false;
        }
        return true;
    }

    private NormalizedMarketSearchResult mapOwnListingToResult(MarketplaceListing listing, String policyId) {
        return new NormalizedMarketSearchResult(
                listing.getExternalListingId(),
                listing.getTitle(),
                listing.getMarketplace(),
                listing.getPrice(),
                listing.getCurrency() != null ? listing.getCurrency() : "BRL",
                "Catálogo Próprio",
                "Próprio",
                listing.getPermalink() != null ? listing.getPermalink() : "https://marketplace.internal/listings/" + listing.getExternalListingId(),
                null,
                "NEW",
                listing.getAvailableQuantity(),
                policyId,
                "OFFICIAL",
                listing.getSyncedAt(),
                "Anúncio próprio do tenant. Métricas de faturamento e vendas são computadas via módulo de pedidos próprios."
        );
    }

    private NormalizedMarketSearchResult mapObservationToResult(MarketObservation obs, String policyId) {
        String seller = obs.getSellerIdentifier() != null ? obs.getSellerIdentifier() : "Vendedor Mercado Livre";
        return new NormalizedMarketSearchResult(
                obs.getObservedListingId(),
                obs.getTitle(),
                obs.getMarketplace(),
                obs.getPrice(),
                obs.getCurrency() != null ? obs.getCurrency() : "BRL",
                seller,
                "Observado Público",
                obs.getSourceUrl(),
                obs.getGtin(),
                obs.getItemCondition() != null ? obs.getItemCondition().name() : "NEW",
                null,
                policyId,
                obs.getSourceType() != null ? obs.getSourceType().name() : "OBSERVED",
                obs.getCollectedAt(),
                NOTICE_UNSUPPORTED_SALES_METRICS
        );
    }

    private void recordAuditQuery(
            UUID tenantId,
            MarketSearchRequest request,
            String policyId,
            String capabilityStatus,
            int resultCount,
            long startTime,
            boolean cached
    ) {
        try {
            long executionTime = System.currentTimeMillis() - startTime;
            MarketSearchQueryRecord record = new MarketSearchQueryRecord(
                    UUID.randomUUID(),
                    tenantId,
                    request.q(),
                    request.marketplace(),
                    request.category(),
                    request.minPrice(),
                    request.maxPrice(),
                    Boolean.TRUE.equals(request.onlyOwnCatalog()),
                    policyId,
                    capabilityStatus,
                    resultCount,
                    executionTime,
                    cached,
                    Instant.now()
            );
            queryRepository.save(record);
        } catch (Exception e) {
            log.error("Erro ao registrar auditoria de consulta para tenant={}: {}", tenantId, e.getMessage());
        }
    }
}
