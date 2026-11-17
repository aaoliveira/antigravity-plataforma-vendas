package br.com.sellerintelligence.market.provider;

import br.com.sellerintelligence.sync.domain.DataProvenance;

import java.time.Instant;
import java.util.List;

public record MarketSearchResult(
        String query,
        int total,
        int offset,
        int limit,
        List<MarketProductDto> items,
        Instant collectedAt,
        DataProvenance provenance
) {}
