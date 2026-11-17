package br.com.sellerintelligence.market.web;

import br.com.sellerintelligence.tenancy.store.Marketplace;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record MarketSearchRequest(
        Marketplace marketplace,
        @NotBlank(message = "query cannot be empty")
        String query,
        String category,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String condition,
        String brand,
        String sort,
        Integer offset,
        Integer limit,
        Boolean saveSnapshot
) {}
