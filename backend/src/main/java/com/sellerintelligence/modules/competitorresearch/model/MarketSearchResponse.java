package com.sellerintelligence.modules.competitorresearch.model;

import com.sellerintelligence.modules.marketplaceadapters.model.CapabilityStatus;

import java.util.List;

/**
 * Resposta estruturada da pesquisa de mercado, incluindo metadados de paginação,
 * proveniência da fonte, status da capacidade, aviso legal e estado de cache.
 */
public record MarketSearchResponse(
        List<NormalizedMarketSearchResult> items,
        long totalElements,
        int totalPages,
        int currentPage,
        int pageSize,
        String sourceUsed,
        String sourcePolicyId,
        CapabilityStatus capabilityStatus,
        boolean cached,
        String capabilityNotice,
        int rateLimitRemaining
) {
    public static MarketSearchResponse empty(
            String sourceUsed,
            String sourcePolicyId,
            CapabilityStatus capabilityStatus,
            String capabilityNotice,
            int rateLimitRemaining
    ) {
        return new MarketSearchResponse(
                List.of(),
                0,
                0,
                0,
                20,
                sourceUsed,
                sourcePolicyId,
                capabilityStatus,
                false,
                capabilityNotice,
                rateLimitRemaining
        );
    }
}
