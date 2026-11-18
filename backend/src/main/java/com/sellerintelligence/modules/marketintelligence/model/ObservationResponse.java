package com.sellerintelligence.modules.marketintelligence.model;

import com.sellerintelligence.modules.marketintelligence.MarketObservation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Resposta de consulta de observação de mercado com proveniência completa.
 */
public record ObservationResponse(
        UUID id,
        UUID tenantId,
        String observedListingId,
        String marketplace,
        String title,
        BigDecimal price,
        String currency,
        ItemCondition itemCondition,
        String sellerIdentifier,
        MarketObservationSourceType sourceType,
        String sourceUrl,
        Instant collectedAt,
        Instant observedAt,
        Instant validUntil,
        String method,
        BigDecimal confidence,
        String methodologyDescription,
        String modelVersion,
        String timeWindow,
        String gtin,
        String brand,
        BigDecimal salesVolume,
        Long visitsCount,
        BigDecimal ctr,
        BigDecimal revenue,
        String minimizedPayload,
        Instant createdAt,
        Instant updatedAt
) {
    public static ObservationResponse from(MarketObservation entity) {
        return new ObservationResponse(
                entity.getId(),
                entity.getTenantId(),
                entity.getObservedListingId(),
                entity.getMarketplace(),
                entity.getTitle(),
                entity.getPrice(),
                entity.getCurrency(),
                entity.getItemCondition(),
                entity.getSellerIdentifier(),
                entity.getSourceType(),
                entity.getSourceUrl(),
                entity.getCollectedAt(),
                entity.getObservedAt(),
                entity.getValidUntil(),
                entity.getMethod(),
                entity.getConfidence(),
                entity.getMethodologyDescription(),
                entity.getModelVersion(),
                entity.getTimeWindow(),
                entity.getGtin(),
                entity.getBrand(),
                entity.getSalesVolume(),
                entity.getVisitsCount(),
                entity.getCtr(),
                entity.getRevenue(),
                entity.getMinimizedPayload(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
