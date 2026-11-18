package com.sellerintelligence.modules.catalog.model;

/**
 * Parâmetros de requisição paginada para busca de anúncios próprios no marketplace.
 */
public record ListingPageRequest(
        int offset,
        int limit
) {
    public ListingPageRequest {
        if (offset < 0) offset = 0;
        if (limit <= 0) limit = 50;
    }
}
