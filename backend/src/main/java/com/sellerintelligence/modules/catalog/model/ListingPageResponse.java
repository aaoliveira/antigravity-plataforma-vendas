package com.sellerintelligence.modules.catalog.model;

import java.util.List;
import java.util.Objects;

/**
 * Resposta paginada contendo itens brutos de anúncios do marketplace.
 */
public record ListingPageResponse(
        List<RawListingItem> items,
        int offset,
        int limit,
        int total,
        boolean hasMore
) {
    public ListingPageResponse {
        items = items != null ? List.copyOf(items) : List.of();
    }
}
