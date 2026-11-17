package br.com.sellerintelligence.market.web;

import br.com.sellerintelligence.market.domain.MarketSearchSnapshot;
import br.com.sellerintelligence.market.domain.SavedMarketProduct;
import br.com.sellerintelligence.market.domain.SavedSearch;
import br.com.sellerintelligence.market.provider.MarketProductDetail;
import br.com.sellerintelligence.market.provider.MarketSearchQuery;
import br.com.sellerintelligence.market.provider.MarketSearchResult;
import br.com.sellerintelligence.market.service.MarketIntelligenceService;
import br.com.sellerintelligence.tenancy.domain.TenantContext;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/market")
public class MarketIntelligenceController {

    private final MarketIntelligenceService marketService;

    public MarketIntelligenceController(MarketIntelligenceService marketService) {
        this.marketService = marketService;
    }

    @PostMapping("/search")
    public ResponseEntity<MarketSearchResult> search(@Valid @RequestBody MarketSearchRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        Marketplace marketplace = request.marketplace() != null ? request.marketplace() : Marketplace.MERCADO_LIVRE;

        MarketSearchQuery query = new MarketSearchQuery(
                request.query(),
                request.category(),
                request.minPrice(),
                request.maxPrice(),
                request.condition(),
                request.brand(),
                request.sort(),
                request.offset() != null ? request.offset() : 0,
                request.limit() != null ? request.limit() : 20
        );

        boolean saveSnapshot = request.saveSnapshot() != null ? request.saveSnapshot() : true;
        MarketSearchResult result = marketService.executeSearch(tenantId, marketplace, query, saveSnapshot);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/items/{marketplace}/{externalItemId}")
    public ResponseEntity<MarketProductDetail> getItemDetail(
            @PathVariable Marketplace marketplace,
            @PathVariable String externalItemId) {
        MarketProductDetail detail = marketService.getProductDetail(marketplace, externalItemId);
        return ResponseEntity.ok(detail);
    }

    // --- Saved Searches ---

    @PostMapping("/saved-searches")
    public ResponseEntity<SavedSearch> saveSearch(@Valid @RequestBody SaveSearchRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        Marketplace marketplace = request.marketplace() != null ? request.marketplace() : Marketplace.MERCADO_LIVRE;

        SavedSearch savedSearch = marketService.saveSearch(
                tenantId,
                request.name(),
                marketplace,
                request.query(),
                request.category(),
                request.minPrice(),
                request.maxPrice(),
                request.condition(),
                request.brand(),
                request.notes()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(savedSearch);
    }

    @GetMapping("/saved-searches")
    public ResponseEntity<List<SavedSearch>> listSavedSearches() {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(marketService.listSavedSearches(tenantId));
    }

    @DeleteMapping("/saved-searches/{id}")
    public ResponseEntity<Void> deleteSavedSearch(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        marketService.deleteSavedSearch(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    // --- Saved Products (Favoritos) ---

    @PostMapping("/saved-products")
    public ResponseEntity<SavedMarketProduct> saveProduct(@Valid @RequestBody SaveProductRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        Marketplace marketplace = request.marketplace() != null ? request.marketplace() : Marketplace.MERCADO_LIVRE;

        SavedMarketProduct product = marketService.saveProduct(
                tenantId,
                marketplace,
                request.externalItemId(),
                request.title(),
                request.price(),
                request.currency() != null ? request.currency() : "BRL",
                request.sellerNickname(),
                request.permalink(),
                request.thumbnailUrl(),
                request.notes()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(product);
    }

    @GetMapping("/saved-products")
    public ResponseEntity<List<SavedMarketProduct>> listSavedProducts() {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(marketService.listSavedProducts(tenantId));
    }

    @PatchMapping("/saved-products/{id}/notes")
    public ResponseEntity<SavedMarketProduct> updateNotes(
            @PathVariable UUID id,
            @RequestBody UpdateNotesRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        SavedMarketProduct product = marketService.updateProductNotes(tenantId, id, request.notes());
        return ResponseEntity.ok(product);
    }

    @DeleteMapping("/saved-products/{id}")
    public ResponseEntity<Void> deleteSavedProduct(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        marketService.deleteSavedProduct(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/snapshots")
    public ResponseEntity<List<MarketSearchSnapshot>> listSnapshots() {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(marketService.listSnapshots(tenantId));
    }
}
