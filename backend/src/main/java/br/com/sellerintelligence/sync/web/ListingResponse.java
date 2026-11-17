package br.com.sellerintelligence.sync.web;

import br.com.sellerintelligence.sync.domain.DataProvenance;
import br.com.sellerintelligence.sync.domain.Listing;
import br.com.sellerintelligence.tenancy.store.Marketplace;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ListingResponse(
        UUID id,
        UUID storeId,
        UUID productId,
        Marketplace marketplace,
        String externalListingId,
        String sku,
        String title,
        String status,
        BigDecimal price,
        String currency,
        Integer availableQuantity,
        Integer soldQuantity,
        String listingType,
        String permalink,
        String thumbnailUrl,
        DataProvenance provenance,
        Instant lastSyncedAt,
        Instant originUpdatedAt
) {
    public static ListingResponse from(Listing l) {
        return new ListingResponse(
                l.getId(),
                l.getStoreId(),
                l.getProductId(),
                l.getMarketplace(),
                l.getExternalListingId(),
                l.getSku(),
                l.getTitle(),
                l.getStatus(),
                l.getPrice(),
                l.getCurrency(),
                l.getAvailableQuantity(),
                l.getSoldQuantity(),
                l.getListingType(),
                l.getPermalink(),
                l.getThumbnailUrl(),
                l.getProvenance(),
                l.getLastSyncedAt(),
                l.getOriginUpdatedAt()
        );
    }
}
