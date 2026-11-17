package br.com.sellerintelligence.market.provider;

import java.math.BigDecimal;

public record MarketSearchQuery(
        String query,
        String category,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String condition,
        String brand,
        String sort,
        int offset,
        int limit
) {
    public MarketSearchQuery {
        if (offset < 0) offset = 0;
        if (limit <= 0 || limit > 50) limit = 20;
    }
}
