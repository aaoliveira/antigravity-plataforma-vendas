package br.com.sellerintelligence.market.service;

import br.com.sellerintelligence.infrastructure.error.BusinessException;
import br.com.sellerintelligence.market.domain.*;
import br.com.sellerintelligence.market.provider.*;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class MarketIntelligenceService {

    private static final Logger log = LoggerFactory.getLogger(MarketIntelligenceService.class);

    private final MarketSearchRegistry searchRegistry;
    private final MarketSearchSnapshotRepository snapshotRepository;
    private final SavedSearchRepository savedSearchRepository;
    private final SavedMarketProductRepository savedProductRepository;

    public MarketIntelligenceService(
            MarketSearchRegistry searchRegistry,
            MarketSearchSnapshotRepository snapshotRepository,
            SavedSearchRepository savedSearchRepository,
            SavedMarketProductRepository savedProductRepository) {
        this.searchRegistry = searchRegistry;
        this.snapshotRepository = snapshotRepository;
        this.savedSearchRepository = savedSearchRepository;
        this.savedProductRepository = savedProductRepository;
    }

    @Transactional
    public MarketSearchResult executeSearch(UUID tenantId, Marketplace marketplace, MarketSearchQuery query, boolean saveSnapshot) {
        MarketSearchProvider provider = searchRegistry.getProvider(marketplace);
        MarketSearchResult result = provider.search(query);

        if (saveSnapshot && result.total() > 0) {
            MarketSearchSnapshot snapshot = new MarketSearchSnapshot(
                    tenantId,
                    marketplace,
                    query.query(),
                    query.category(),
                    query.minPrice(),
                    query.maxPrice(),
                    query.condition(),
                    query.brand(),
                    query.sort(),
                    result.total(),
                    result.collectedAt(),
                    result.provenance()
            );

            for (MarketProductDto item : result.items()) {
                MarketProductItem entity = new MarketProductItem(
                        snapshot,
                        marketplace,
                        item.externalId(),
                        item.title(),
                        item.price(),
                        item.currency(),
                        item.condition(),
                        item.sellerNickname(),
                        item.sellerReputation(),
                        item.permalink(),
                        item.thumbnailUrl(),
                        item.categoryId(),
                        item.categoryName(),
                        item.brand(),
                        item.freeShipping(),
                        item.reviewScore(),
                        item.reviewCount(),
                        item.estimatedSalesRange(),
                        item.estimationMethod(),
                        item.provenance(),
                        item.collectedAt()
                );
                snapshot.addItem(entity);
            }

            snapshotRepository.save(snapshot);
            log.info("Saved market search snapshot: id={}, tenant={}, items={}", snapshot.getId(), tenantId, snapshot.getItems().size());
        }

        return result;
    }

    @Transactional(readOnly = true)
    public MarketProductDetail getProductDetail(Marketplace marketplace, String externalItemId) {
        MarketSearchProvider provider = searchRegistry.getProvider(marketplace);
        return provider.getItemDetail(externalItemId);
    }

    // --- Saved Searches ---

    @Transactional
    public SavedSearch saveSearch(UUID tenantId, String name, Marketplace marketplace, String query,
                                  String category, BigDecimal minPrice, BigDecimal maxPrice,
                                  String condition, String brand, String notes) {
        SavedSearch savedSearch = new SavedSearch(
                tenantId, name, marketplace, query, category, minPrice, maxPrice, condition, brand, notes
        );
        return savedSearchRepository.save(savedSearch);
    }

    @Transactional(readOnly = true)
    public List<SavedSearch> listSavedSearches(UUID tenantId) {
        return savedSearchRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    @Transactional
    public void deleteSavedSearch(UUID tenantId, UUID searchId) {
        SavedSearch search = savedSearchRepository.findByIdAndTenantId(searchId, tenantId)
                .orElseThrow(() -> new BusinessException("SEARCH_NOT_FOUND", "Saved search not found or access denied", HttpStatus.NOT_FOUND));
        savedSearchRepository.delete(search);
    }

    // --- Saved Products (Oportunidades & Notas) ---

    @Transactional
    public SavedMarketProduct saveProduct(UUID tenantId, Marketplace marketplace, String externalItemId,
                                         String title, BigDecimal price, String currency,
                                         String sellerNickname, String permalink, String thumbnailUrl,
                                         String notes) {
        SavedMarketProduct existing = savedProductRepository.findByTenantIdAndMarketplaceAndExternalItemId(tenantId, marketplace, externalItemId)
                .orElse(null);

        if (existing != null) {
            existing.updateNotes(notes);
            return savedProductRepository.save(existing);
        }

        SavedMarketProduct product = new SavedMarketProduct(
                tenantId, marketplace, externalItemId, title, price, currency,
                sellerNickname, permalink, thumbnailUrl, notes,
                br.com.sellerintelligence.sync.domain.DataProvenance.OBSERVED
        );
        return savedProductRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<SavedMarketProduct> listSavedProducts(UUID tenantId) {
        return savedProductRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    @Transactional
    public SavedMarketProduct updateProductNotes(UUID tenantId, UUID productId, String notes) {
        SavedMarketProduct product = savedProductRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Saved product not found or access denied", HttpStatus.NOT_FOUND));
        product.updateNotes(notes);
        return savedProductRepository.save(product);
    }

    @Transactional
    public void deleteSavedProduct(UUID tenantId, UUID productId) {
        SavedMarketProduct product = savedProductRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new BusinessException("PRODUCT_NOT_FOUND", "Saved product not found or access denied", HttpStatus.NOT_FOUND));
        savedProductRepository.delete(product);
    }

    @Transactional(readOnly = true)
    public List<MarketSearchSnapshot> listSnapshots(UUID tenantId) {
        return snapshotRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }
}
