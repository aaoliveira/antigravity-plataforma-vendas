package br.com.sellerintelligence.sync.web;

import br.com.sellerintelligence.tenancy.domain.TenantContext;
import br.com.sellerintelligence.sync.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class CatalogAndOrdersController {

    private final ProductRepository productRepository;
    private final ListingRepository listingRepository;
    private final OrderRepository orderRepository;
    private final SyncJobRepository syncJobRepository;

    public CatalogAndOrdersController(
            ProductRepository productRepository,
            ListingRepository listingRepository,
            OrderRepository orderRepository,
            SyncJobRepository syncJobRepository) {
        this.productRepository = productRepository;
        this.listingRepository = listingRepository;
        this.orderRepository = orderRepository;
        this.syncJobRepository = syncJobRepository;
    }

    @GetMapping("/catalog/products")
    public ResponseEntity<List<ProductResponse>> getProducts() {
        UUID tenantId = TenantContext.getTenantId();
        List<ProductResponse> products = productRepository.findByTenantId(tenantId).stream()
                .map(ProductResponse::from)
                .toList();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/catalog/listings")
    public ResponseEntity<List<ListingResponse>> getListings() {
        UUID tenantId = TenantContext.getTenantId();
        List<ListingResponse> listings = listingRepository.findByTenantId(tenantId).stream()
                .map(ListingResponse::from)
                .toList();
        return ResponseEntity.ok(listings);
    }

    @GetMapping("/orders")
    public ResponseEntity<List<OrderResponse>> getOrders() {
        UUID tenantId = TenantContext.getTenantId();
        List<OrderResponse> orders = orderRepository.findByTenantIdOrderByOrderDateDesc(tenantId).stream()
                .map(OrderResponse::from)
                .toList();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/sync/stats")
    public ResponseEntity<SyncStatsResponse> getSyncStats() {
        UUID tenantId = TenantContext.getTenantId();
        long totalProducts = productRepository.countByTenantId(tenantId);
        long totalListings = listingRepository.countByTenantId(tenantId);
        long totalOrders = orderRepository.countByTenantId(tenantId);

        Instant lastSyncAt = syncJobRepository.findTopByTenantIdOrderByCreatedAtDesc(tenantId)
                .map(SyncJob::getCompletedAt)
                .orElse(null);

        return ResponseEntity.ok(new SyncStatsResponse(totalProducts, totalListings, totalOrders, lastSyncAt));
    }
}
