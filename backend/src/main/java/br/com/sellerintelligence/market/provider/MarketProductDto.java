package br.com.sellerintelligence.market.provider;

import br.com.sellerintelligence.sync.domain.DataProvenance;

import java.math.BigDecimal;
import java.time.Instant;

public record MarketProductDto(
        String externalId,
        String title,
        BigDecimal price,
        String currency,
        String condition,
        String sellerNickname,
        String sellerReputation,
        String permalink,
        String thumbnailUrl,
        String categoryId,
        String categoryName,
        String brand,
        Boolean freeShipping,
        BigDecimal reviewScore,
        Integer reviewCount,
        String estimatedSalesRange,
        String estimationMethod,
        DataProvenance provenance,
        Instant collectedAt
) {}
