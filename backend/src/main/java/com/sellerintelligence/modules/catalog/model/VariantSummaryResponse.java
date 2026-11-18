package com.sellerintelligence.modules.catalog.model;

import java.util.UUID;

public record VariantSummaryResponse(
        UUID id,
        String variantSku,
        String gtinBarcode,
        String attributesJson
) {}
