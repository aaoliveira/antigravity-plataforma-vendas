package com.sellerintelligence.modules.reporting.provider;

import com.sellerintelligence.modules.catalog.MarketplaceListing;
import com.sellerintelligence.modules.catalog.MarketplaceListingRepository;
import com.sellerintelligence.modules.reporting.model.ExportDataPage;
import com.sellerintelligence.modules.reporting.model.ExportDataset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CatalogExportDataProvider implements ExportDataProvider {

    private static final List<String> HEADERS = List.of(
            "ID Anúncio",
            "ID Externo",
            "SKU Externo",
            "Título",
            "Marketplace",
            "ID Conta",
            "Preço (BRL)",
            "Estoque",
            "Status",
            "Link Externo"
    );

    private final MarketplaceListingRepository repository;

    public CatalogExportDataProvider(MarketplaceListingRepository repository) {
        this.repository = repository;
    }

    @Override
    public ExportDataset supportedDataset() {
        return ExportDataset.CATALOG;
    }

    @Override
    public ExportDataPage fetchTenantData(UUID tenantId, Map<String, Object> filterParams, int maxRows) {
        String marketplace = parseString(filterParams.get("marketplace"));
        UUID sellerAccountId = parseUuid(filterParams.get("sellerAccountId"));
        String status = parseString(filterParams.get("status"));
        String search = parseString(filterParams.get("search"));

        int limit = Math.max(1, Math.min(maxRows, 50000));
        Page<MarketplaceListing> page = repository.searchListings(
                tenantId,
                marketplace,
                sellerAccountId,
                status,
                search,
                PageRequest.of(0, limit)
        );

        List<List<Object>> rows = new ArrayList<>();
        for (MarketplaceListing listing : page.getContent()) {
            rows.add(List.of(
                    listing.getId().toString(),
                    listing.getExternalListingId() != null ? listing.getExternalListingId() : "",
                    listing.getExternalSku() != null ? listing.getExternalSku() : "",
                    listing.getTitle() != null ? listing.getTitle() : "",
                    listing.getMarketplace() != null ? listing.getMarketplace() : "",
                    listing.getSellerAccountId() != null ? listing.getSellerAccountId().toString() : "",
                    listing.getPrice() != null ? listing.getPrice() : "",
                    listing.getAvailableQuantity() != null ? listing.getAvailableQuantity() : 0,
                    listing.getStatus() != null ? listing.getStatus() : "",
                    listing.getPermalink() != null ? listing.getPermalink() : ""
            ));
        }

        boolean isTruncated = page.getTotalElements() > limit;
        return new ExportDataPage(HEADERS, rows, (int) page.getTotalElements(), isTruncated);
    }

    private UUID parseUuid(Object val) {
        if (val instanceof UUID u) return u;
        if (val instanceof String s && !s.isBlank()) {
            try { return UUID.fromString(s); } catch (Exception ignored) {}
        }
        return null;
    }

    private String parseString(Object val) {
        if (val instanceof String s && !s.isBlank()) return s;
        return null;
    }
}
