package com.sellerintelligence.modules.catalog;

import com.sellerintelligence.core.exception.ResourceNotFoundException;
import com.sellerintelligence.core.tenant.TenantContextHolder;
import com.sellerintelligence.modules.catalog.model.*;
import com.sellerintelligence.modules.catalog.service.CatalogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST Controller oficial para consulta do catálogo de produtos e anúncios integrados (P007-FE).
 * Garante filtros server-side, busca textual, paginação e isolamento estrito de tenant.
 */
@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/listings")
    public ResponseEntity<PageResponse<ListingDetailResponse>> searchListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String marketplace,
            @RequestParam(required = false) UUID sellerAccountId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();

        // Limita o tamanho máximo de página para proteção contra DoS
        int safeSize = Math.min(Math.max(1, size), 100);
        int safePage = Math.max(0, page);

        Page<MarketplaceListing> listingPage = catalogService.searchListings(
                tenantId,
                marketplace,
                sellerAccountId,
                status,
                search,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "syncedAt"))
        );

        // Pré-carrega contas do tenant para resolução eficiente de nomes
        Map<UUID, String> accountNames = catalogService.listSellerAccounts(tenantId).stream()
                .collect(Collectors.toMap(SellerAccount::getId, SellerAccount::getAccountName, (a, b) -> a));

        List<ListingDetailResponse> content = listingPage.getContent().stream()
                .map(listing -> mapToDetail(tenantId, listing, accountNames))
                .toList();

        return ResponseEntity.ok(new PageResponse<>(
                content,
                listingPage.getNumber(),
                listingPage.getSize(),
                listingPage.getTotalElements(),
                listingPage.getTotalPages(),
                listingPage.isLast()
        ));
    }

    @GetMapping("/listings/{id}")
    public ResponseEntity<ListingDetailResponse> getListingById(@PathVariable UUID id) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();

        MarketplaceListing listing = catalogService.getListing(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Anúncio não encontrado para o tenant ativo"));

        Map<UUID, String> accountNames = catalogService.listSellerAccounts(tenantId).stream()
                .collect(Collectors.toMap(SellerAccount::getId, SellerAccount::getAccountName, (a, b) -> a));

        return ResponseEntity.ok(mapToDetail(tenantId, listing, accountNames));
    }

    @GetMapping("/seller-accounts")
    public ResponseEntity<List<SellerAccountResponse>> listSellerAccounts() {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();

        List<SellerAccountResponse> accounts = catalogService.listSellerAccounts(tenantId).stream()
                .map(acc -> new SellerAccountResponse(
                        acc.getId(),
                        acc.getMarketplace(),
                        acc.getExternalAccountId(),
                        acc.getAccountName(),
                        acc.getStatus()
                ))
                .toList();

        return ResponseEntity.ok(accounts);
    }

    private ListingDetailResponse mapToDetail(
            UUID tenantId,
            MarketplaceListing listing,
            Map<UUID, String> accountNames
    ) {
        String accountName = accountNames.getOrDefault(listing.getSellerAccountId(), "Conta Conectada");

        ProductSummaryResponse productSummary = null;
        if (listing.getProductId() != null) {
            productSummary = catalogService.getProduct(tenantId, listing.getProductId())
                    .map(p -> new ProductSummaryResponse(
                            p.getId(),
                            p.getTitle(),
                            p.getCanonicalSku(),
                            p.getBrand(),
                            p.getCategory(),
                            p.getStatus()
                    ))
                    .orElse(null);
        }

        VariantSummaryResponse variantSummary = null;
        if (listing.getProductVariantId() != null) {
            variantSummary = catalogService.getVariant(tenantId, listing.getProductVariantId())
                    .map(v -> new VariantSummaryResponse(
                            v.getId(),
                            v.getVariantSku(),
                            v.getGtinBarcode(),
                            v.getAttributesJson()
                    ))
                    .orElse(null);
        }

        return new ListingDetailResponse(
                listing.getId(),
                listing.getSellerAccountId(),
                accountName,
                listing.getMarketplace(),
                listing.getExternalListingId(),
                listing.getExternalSku(),
                listing.getExternalVariationId(),
                listing.getTitle(),
                listing.getPrice(),
                listing.getCurrency(),
                listing.getAvailableQuantity(),
                listing.getStatus(),
                listing.getPermalink(),
                listing.getOrigin(),
                listing.getSchemaVersion(),
                listing.getSyncedAt(),
                productSummary,
                variantSummary
        );
    }
}
