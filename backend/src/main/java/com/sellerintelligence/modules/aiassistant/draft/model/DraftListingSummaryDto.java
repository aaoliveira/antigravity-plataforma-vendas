package com.sellerintelligence.modules.aiassistant.draft.model;

import com.sellerintelligence.modules.aiassistant.draft.entity.DraftListing;

import java.time.Instant;
import java.util.UUID;

public record DraftListingSummaryDto(
        UUID id,
        UUID tenantId,
        UUID productId,
        UUID productVariantId,
        String targetMarketplace,
        int currentVersionNumber,
        String status,
        String publicationStatus,
        boolean externalPublishBlocked,
        Instant createdAt,
        Instant updatedAt
) {
    public static DraftListingSummaryDto from(DraftListing entity) {
        return new DraftListingSummaryDto(
                entity.getId(),
                entity.getTenantId(),
                entity.getProductId(),
                entity.getProductVariantId(),
                entity.getTargetMarketplace(),
                entity.getCurrentVersionNumber(),
                entity.getStatus(),
                "NOT_PUBLISHED_DRAFT_ONLY",
                true,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
