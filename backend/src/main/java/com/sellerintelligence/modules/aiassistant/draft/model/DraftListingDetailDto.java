package com.sellerintelligence.modules.aiassistant.draft.model;

import com.sellerintelligence.modules.aiassistant.draft.entity.DraftListing;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DraftListingDetailDto(
        UUID id,
        UUID tenantId,
        UUID productId,
        UUID productVariantId,
        String targetMarketplace,
        int currentVersionNumber,
        String status,
        String publicationStatus,
        boolean externalPublishBlocked,
        DraftListingVersionDto currentVersion,
        List<DraftListingVersionDto> versionHistory,
        MarketplaceRuleLimitsDto marketplaceLimits,
        Instant createdAt,
        Instant updatedAt
) {
    public static DraftListingDetailDto of(
            DraftListing entity,
            DraftListingVersionDto currentVersion,
            List<DraftListingVersionDto> history,
            MarketplaceRuleLimitsDto limits
    ) {
        return new DraftListingDetailDto(
                entity.getId(),
                entity.getTenantId(),
                entity.getProductId(),
                entity.getProductVariantId(),
                entity.getTargetMarketplace(),
                entity.getCurrentVersionNumber(),
                entity.getStatus(),
                "NOT_PUBLISHED_DRAFT_ONLY",
                true,
                currentVersion,
                history != null ? List.copyOf(history) : List.of(),
                limits,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
