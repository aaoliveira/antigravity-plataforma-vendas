package com.sellerintelligence.modules.catalog.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Representa um item bruto de anúncio extraído do adapter oficial do marketplace.
 * Contém apenas dados autorizados de catálogo próprio (READ_OWN_CATALOG) com minimização de payload.
 */
public record RawListingItem(
        String externalListingId,
        String title,
        BigDecimal price,
        int availableQuantity,
        String status,
        String permalink,
        String externalSku,
        String gtin,
        String rawJson,
        String origin,
        String schemaVersion
) {
    public RawListingItem {
        Objects.requireNonNull(externalListingId, "externalListingId não pode ser nulo");
        Objects.requireNonNull(title, "title não pode ser nulo");
        Objects.requireNonNull(price, "price não pode ser nulo");
        status = status != null ? status : "ACTIVE";
        origin = origin != null ? origin : "UNKNOWN";
        schemaVersion = schemaVersion != null ? schemaVersion : "v1";
    }
}
