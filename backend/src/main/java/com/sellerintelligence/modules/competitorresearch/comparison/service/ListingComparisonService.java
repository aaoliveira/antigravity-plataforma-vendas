package com.sellerintelligence.modules.competitorresearch.comparison.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellerintelligence.core.exception.ResourceNotFoundException;
import com.sellerintelligence.modules.catalog.MarketplaceListing;
import com.sellerintelligence.modules.catalog.MarketplaceListingRepository;
import com.sellerintelligence.modules.competitorresearch.comparison.entity.ListingComparisonRecord;
import com.sellerintelligence.modules.competitorresearch.comparison.model.ComparableListingItem;
import com.sellerintelligence.modules.competitorresearch.comparison.model.CompareListingsRequest;
import com.sellerintelligence.modules.competitorresearch.comparison.model.CompatibilityStatus;
import com.sellerintelligence.modules.competitorresearch.comparison.model.EquivalenceScoreBreakdown;
import com.sellerintelligence.modules.competitorresearch.comparison.model.FindEquivalentsRequest;
import com.sellerintelligence.modules.competitorresearch.comparison.model.ItemCondition;
import com.sellerintelligence.modules.competitorresearch.comparison.model.ListingComparisonResult;
import com.sellerintelligence.modules.competitorresearch.comparison.model.PriceComparisonResult;
import com.sellerintelligence.modules.competitorresearch.comparison.model.SavedComparisonDto;
import com.sellerintelligence.modules.competitorresearch.comparison.repository.ListingComparisonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Orquestrador de serviços de comparação de listings equivalentes,
 * cálculo de variações temporais de preço e salvamento de comparações auditadas.
 */
@Service
public class ListingComparisonService {

    private static final Logger log = LoggerFactory.getLogger(ListingComparisonService.class);

    private final ListingEquivalenceService equivalenceService;
    private final ListingPriceComparisonService priceComparisonService;
    private final ListingComparisonRepository comparisonRepository;
    private final MarketplaceListingRepository marketplaceListingRepository;
    private final ObjectMapper objectMapper;

    public ListingComparisonService(
            ListingEquivalenceService equivalenceService,
            ListingPriceComparisonService priceComparisonService,
            ListingComparisonRepository comparisonRepository,
            MarketplaceListingRepository marketplaceListingRepository,
            ObjectMapper objectMapper
    ) {
        this.equivalenceService = equivalenceService;
        this.priceComparisonService = priceComparisonService;
        this.comparisonRepository = comparisonRepository;
        this.marketplaceListingRepository = marketplaceListingRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ListingComparisonResult compareListings(UUID tenantId, CompareListingsRequest request) {
        Objects.requireNonNull(tenantId, "tenantId é obrigatório");
        Objects.requireNonNull(request, "request é obrigatório");

        ComparableListingItem base = resolveBaseItem(tenantId, request.baseListing(), request.baseListingId());
        ComparableListingItem target = resolveTargetItem(tenantId, request.targetListing(), request.targetListingId());

        EquivalenceScoreBreakdown equivalence = equivalenceService.calculateEquivalence(
                base, target, request.threshold()
        );

        PriceComparisonResult priceComp = priceComparisonService.comparePrices(
                base, target, request.maxComparableDays()
        );

        ListingComparisonResult result = new ListingComparisonResult(
                base, target, equivalence, priceComp, Instant.now()
        );

        if (Boolean.TRUE.equals(request.persistComparison())) {
            persistComparison(tenantId, result);
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<ListingComparisonResult> findEquivalents(UUID tenantId, FindEquivalentsRequest request) {
        Objects.requireNonNull(tenantId, "tenantId é obrigatório");
        Objects.requireNonNull(request, "request é obrigatório");

        ComparableListingItem base = resolveBaseItem(tenantId, request.baseListing(), request.baseListingId());
        List<ComparableListingItem> candidates = request.candidates();

        List<ListingComparisonResult> results = new ArrayList<>();

        for (ComparableListingItem candidate : candidates) {
            EquivalenceScoreBreakdown equivalence = equivalenceService.calculateEquivalence(
                    base, candidate, request.threshold()
            );

            PriceComparisonResult priceComp = priceComparisonService.comparePrices(
                    base, candidate, request.maxComparableDays()
            );

            results.add(new ListingComparisonResult(
                    base, candidate, equivalence, priceComp, Instant.now()
            ));
        }

        // Ordena: compatíveis primeiro, depois por score de similaridade decrescente
        results.sort(Comparator
                .comparing((ListingComparisonResult r) -> r.similarity().isCompatible())
                .reversed()
                .thenComparing(r -> r.similarity().overallScore(), Comparator.reverseOrder())
        );

        return results;
    }

    @Transactional
    public SavedComparisonDto saveComparison(UUID tenantId, ListingComparisonResult result) {
        ListingComparisonRecord saved = persistComparison(tenantId, result);
        return mapToSavedDto(saved);
    }

    @Transactional(readOnly = true)
    public Page<SavedComparisonDto> listSavedComparisons(UUID tenantId, Pageable pageable) {
        return comparisonRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId, pageable)
                .map(this::mapToSavedDto);
    }

    private ListingComparisonRecord persistComparison(UUID tenantId, ListingComparisonResult result) {
        String reasonsJson = null;
        try {
            reasonsJson = objectMapper.writeValueAsString(result.similarity().incompatibilityReasons());
        } catch (Exception e) {
            log.warn("Falha ao serializar motivos de incompatibilidade para JSON", e);
        }

        ListingComparisonRecord record = new ListingComparisonRecord(
                UUID.randomUUID(),
                tenantId,
                result.baseListing().listingId(),
                result.baseListing().title(),
                result.baseListing().brand(),
                result.baseListing().model(),
                result.baseListing().gtin(),
                result.baseListing().price(),
                result.baseListing().shippingCost(),
                result.baseListing().observedAt(),
                result.baseListing().marketplace(),
                result.targetListing().listingId(),
                result.targetListing().title(),
                result.targetListing().brand(),
                result.targetListing().model(),
                result.targetListing().gtin(),
                result.targetListing().price(),
                result.targetListing().shippingCost(),
                result.targetListing().observedAt(),
                result.targetListing().marketplace(),
                result.targetListing().sellerName(),
                result.targetListing().sourceUrl(),
                result.similarity().overallScore(),
                result.similarity().gtinScore(),
                result.similarity().brandScore(),
                result.similarity().modelScore(),
                result.similarity().titleScore(),
                result.similarity().attributesScore(),
                result.similarity().isEquivalent(),
                result.similarity().isCompatible(),
                result.similarity().compatibilityStatus().name(),
                reasonsJson,
                result.priceComparison().isTemporalWindowComparable(),
                result.priceComparison().priceDifference(),
                result.priceComparison().priceVariationPercentage(),
                result.priceComparison().timeDeltaDays(),
                result.similarity().explanation()
        );

        return comparisonRepository.save(record);
    }

    private ComparableListingItem resolveBaseItem(UUID tenantId, ComparableListingItem item, String idStr) {
        if (item != null) {
            return item;
        }
        if (idStr != null && !idStr.isBlank()) {
            try {
                UUID listingUuid = UUID.fromString(idStr);
                Optional<MarketplaceListing> ownListing = marketplaceListingRepository.findByIdAndTenantId(listingUuid, tenantId);
                if (ownListing.isPresent()) {
                    MarketplaceListing l = ownListing.get();
                    return ComparableListingItem.builder()
                            .listingId(l.getId().toString())
                            .title(l.getTitle())
                            .price(l.getPrice())
                            .marketplace(l.getMarketplace())
                            .observedAt(l.getSyncedAt())
                            .isOwnListing(true)
                            .condition(ItemCondition.NEW)
                            .build();
                }
            } catch (IllegalArgumentException ignored) {
                // not a UUID, proceed
            }
        }
        throw new ResourceNotFoundException("Item base não fornecido ou não encontrado para o tenant");
    }

    private ComparableListingItem resolveTargetItem(UUID tenantId, ComparableListingItem item, String idStr) {
        if (item != null) {
            return item;
        }
        if (idStr != null && !idStr.isBlank()) {
            try {
                UUID listingUuid = UUID.fromString(idStr);
                Optional<MarketplaceListing> ownListing = marketplaceListingRepository.findByIdAndTenantId(listingUuid, tenantId);
                if (ownListing.isPresent()) {
                    MarketplaceListing l = ownListing.get();
                    return ComparableListingItem.builder()
                            .listingId(l.getId().toString())
                            .title(l.getTitle())
                            .price(l.getPrice())
                            .marketplace(l.getMarketplace())
                            .observedAt(l.getSyncedAt())
                            .isOwnListing(true)
                            .condition(ItemCondition.NEW)
                            .build();
                }
            } catch (IllegalArgumentException ignored) {
                // not a UUID, proceed
            }
        }
        throw new ResourceNotFoundException("Item alvo não fornecido ou não encontrado para o tenant");
    }

    private SavedComparisonDto mapToSavedDto(ListingComparisonRecord r) {
        List<String> reasons = Collections.emptyList();
        if (r.getIncompatibilityReasonsJson() != null && !r.getIncompatibilityReasonsJson().isBlank()) {
            try {
                reasons = objectMapper.readValue(r.getIncompatibilityReasonsJson(), new TypeReference<>() {});
            } catch (Exception ignored) {
            }
        }

        CompatibilityStatus compStatus;
        try {
            compStatus = CompatibilityStatus.valueOf(r.getCompatibilityStatus());
        } catch (Exception e) {
            compStatus = CompatibilityStatus.COMPATIBLE;
        }

        return new SavedComparisonDto(
                r.getId(),
                r.getTenantId(),
                r.getBaseListingId(),
                r.getBaseTitle(),
                r.getBaseBrand(),
                r.getBaseMarketplace(),
                r.getBasePrice(),
                r.getBaseShippingCost(),
                r.getTargetListingId(),
                r.getTargetTitle(),
                r.getTargetBrand(),
                r.getTargetMarketplace(),
                r.getTargetPrice(),
                r.getTargetShippingCost(),
                r.getSimilarityScore(),
                r.isEquivalent(),
                r.isCompatible(),
                compStatus,
                reasons,
                r.isPriceVariationComparable(),
                r.getPriceDifference(),
                r.getPriceVariationPercentage(),
                r.getTimeDeltaDays(),
                r.getExplanation(),
                r.getCreatedAt()
        );
    }
}
