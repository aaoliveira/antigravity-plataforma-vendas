package com.sellerintelligence.modules.competitorresearch.comparison.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

/**
 * Representação unificada de um listing (anúncio próprio ou concorrente)
 * preparado para análise comparativa de equivalência, preços e atributos.
 */
public record ComparableListingItem(
        String listingId,
        String title,
        String brand,
        String model,
        String gtin,
        String category,
        ItemCondition condition,
        Map<String, String> attributes,
        BigDecimal price,
        String currency,
        BigDecimal shippingCost,
        ShippingStatus shippingStatus,
        BigDecimal finalPriceWithShipping,
        Instant observedAt,
        String marketplace,
        String sellerName,
        String sourceUrl,
        boolean isOwnListing
) {
    public ComparableListingItem {
        if (attributes == null) {
            attributes = Collections.emptyMap();
        }
        if (currency == null || currency.isBlank()) {
            currency = "BRL";
        }
        if (condition == null) {
            condition = ItemCondition.NOT_SPECIFIED;
        }
        if (shippingCost != null) {
            shippingStatus = ShippingStatus.KNOWN;
            if (price != null) {
                finalPriceWithShipping = price.add(shippingCost);
            }
        } else {
            shippingStatus = ShippingStatus.UNKNOWN;
            finalPriceWithShipping = null; // FRETE DESCONHECIDO NUNCA INFERIDO
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String listingId;
        private String title;
        private String brand;
        private String model;
        private String gtin;
        private String category;
        private ItemCondition condition = ItemCondition.NOT_SPECIFIED;
        private Map<String, String> attributes = Collections.emptyMap();
        private BigDecimal price;
        private String currency = "BRL";
        private BigDecimal shippingCost;
        private Instant observedAt = Instant.now();
        private String marketplace = "MERCADO_LIVRE";
        private String sellerName;
        private String sourceUrl;
        private boolean isOwnListing = false;

        public Builder listingId(String listingId) { this.listingId = listingId; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder brand(String brand) { this.brand = brand; return this; }
        public Builder model(String model) { this.model = model; return this; }
        public Builder gtin(String gtin) { this.gtin = gtin; return this; }
        public Builder category(String category) { this.category = category; return this; }
        public Builder condition(ItemCondition condition) { this.condition = condition; return this; }
        public Builder attributes(Map<String, String> attributes) { this.attributes = attributes; return this; }
        public Builder price(BigDecimal price) { this.price = price; return this; }
        public Builder currency(String currency) { this.currency = currency; return this; }
        public Builder shippingCost(BigDecimal shippingCost) { this.shippingCost = shippingCost; return this; }
        public Builder observedAt(Instant observedAt) { this.observedAt = observedAt; return this; }
        public Builder marketplace(String marketplace) { this.marketplace = marketplace; return this; }
        public Builder sellerName(String sellerName) { this.sellerName = sellerName; return this; }
        public Builder sourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; return this; }
        public Builder isOwnListing(boolean isOwnListing) { this.isOwnListing = isOwnListing; return this; }

        public ComparableListingItem build() {
            return new ComparableListingItem(
                    listingId, title, brand, model, gtin, category, condition,
                    attributes, price, currency, shippingCost, null, null,
                    observedAt, marketplace, sellerName, sourceUrl, isOwnListing
            );
        }
    }
}
