package com.sellerintelligence.modules.competitorresearch.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO para histórico e trilha de auditoria das pesquisas realizadas pelo tenant.
 */
public record MarketSearchHistoryItemDto(
        UUID id,
        String queryText,
        String marketplace,
        String category,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        boolean onlyOwnCatalog,
        String sourcePolicyId,
        String capabilityStatus,
        int resultCount,
        long executionTimeMs,
        boolean cached,
        Instant executedAt
) {
    public static MarketSearchHistoryItemDto fromEntity(MarketSearchQueryRecord record) {
        return new MarketSearchHistoryItemDto(
                record.getId(),
                record.getQueryText(),
                record.getMarketplace(),
                record.getCategory(),
                record.getMinPrice(),
                record.getMaxPrice(),
                record.isOnlyOwnCatalog(),
                record.getSourcePolicyId(),
                record.getCapabilityStatus(),
                record.getResultCount(),
                record.getExecutionTimeMs(),
                record.isCached(),
                record.getExecutedAt()
        );
    }
}
