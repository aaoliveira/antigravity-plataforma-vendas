package com.sellerintelligence.modules.listinganalytics;

import com.sellerintelligence.modules.listinganalytics.model.MetricCatalogEntry;
import com.sellerintelligence.modules.listinganalytics.model.MetricScope;
import com.sellerintelligence.modules.listinganalytics.model.OfficialMetricCatalog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class OfficialMetricCatalogTest {

    private OfficialMetricCatalog catalog;

    @BeforeEach
    void setUp() {
        catalog = new OfficialMetricCatalog();
    }

    @Test
    @DisplayName("Catálogo: Deve inventariar métricas oficiais do Mercado Livre com endpoints e limites comprovados")
    void shouldContainMercadoLivreOfficialMetrics() {
        Optional<MetricCatalogEntry> visits = catalog.findByMarketplaceAndKey("MERCADO_LIVRE", "VISITS");
        assertThat(visits).isPresent();
        assertThat(visits.get().supportedByChannel()).isTrue();
        assertThat(visits.get().sourceEndpoint()).contains("/items/{id}/visits");
        assertThat(visits.get().rateLimitsAndQuotas()).contains("100 req/min");
        assertThat(visits.get().maxWindowDays()).isEqualTo(365);

        Optional<MetricCatalogEntry> orders = catalog.findByMarketplaceAndKey("MERCADO_LIVRE", "ORDERS_COUNT");
        assertThat(orders).isPresent();
        assertThat(orders.get().supportedByChannel()).isTrue();
        assertThat(orders.get().sourceEndpoint()).contains("/orders/search");
    }

    @Test
    @DisplayName("Catálogo: Deve documentar IMPRESSIONS e CLICKS como não suportados em tráfego orgânico no Mercado Livre")
    void shouldDocumentMercadoLivreOrganicImpressionsAsUnsupported() {
        Optional<MetricCatalogEntry> impressions = catalog.findByMarketplaceAndKey("MERCADO_LIVRE", "IMPRESSIONS");
        assertThat(impressions).isPresent();
        assertThat(impressions.get().supportedByChannel()).isFalse();
        assertThat(impressions.get().unavailabilityReason()).contains("Mercado Ads");

        Optional<MetricCatalogEntry> clicks = catalog.findByMarketplaceAndKey("MERCADO_LIVRE", "CLICKS");
        assertThat(clicks).isPresent();
        assertThat(clicks.get().supportedByChannel()).isFalse();
        assertThat(clicks.get().unavailabilityReason()).contains("Mercado Ads");

        Optional<MetricCatalogEntry> ctr = catalog.findByMarketplaceAndKey("MERCADO_LIVRE", "CTR");
        assertThat(ctr).isPresent();
        assertThat(ctr.get().supportedByChannel()).isFalse();
        assertThat(ctr.get().unavailabilityReason()).contains("Indisponível no tráfego orgânico");
    }

    @Test
    @DisplayName("Catálogo: Deve documentar Shopee OpenAPI v2 analytics e ads")
    void shouldContainShopeeMetrics() {
        Optional<MetricCatalogEntry> pageViews = catalog.findByMarketplaceAndKey("SHOPEE", "PAGE_VIEWS");
        assertThat(pageViews).isPresent();
        assertThat(pageViews.get().supportedByChannel()).isTrue();
        assertThat(pageViews.get().sourceEndpoint()).contains("v2.analytics");

        Optional<MetricCatalogEntry> uniqueVisitors = catalog.findByMarketplaceAndKey("SHOPEE", "UNIQUE_VISITORS");
        assertThat(uniqueVisitors).isPresent();
        assertThat(uniqueVisitors.get().supportedByChannel()).isTrue();

        Optional<MetricCatalogEntry> ctr = catalog.findByMarketplaceAndKey("SHOPEE", "CTR");
        assertThat(ctr).isPresent();
        assertThat(ctr.get().supportedByChannel()).isTrue();
        assertThat(ctr.get().formulaDefinition()).isEqualTo("clicks / impressions * 100");
    }

    @Test
    @DisplayName("Catálogo: Métricas de concorrentes devem ser categoricamente UNSUPPORTED (Anti-Alucinação)")
    void shouldMarkCompetitorSalesAsUnsupported() {
        Optional<MetricCatalogEntry> compSales = catalog.findByMarketplaceAndKey("ALL", "COMPETITOR_SALES_VOLUME");
        assertThat(compSales).isPresent();
        assertThat(compSales.get().supportedByChannel()).isFalse();
        assertThat(compSales.get().unavailabilityReason()).contains("Nenhum marketplace homologado disponibiliza vendas");
    }

    @Test
    @DisplayName("Catálogo: Deve filtrar por marketplace")
    void shouldFilterCatalogByMarketplace() {
        List<MetricCatalogEntry> meliEntries = catalog.findByMarketplace("MERCADO_LIVRE");
        assertThat(meliEntries).isNotEmpty();
        assertThat(meliEntries).allMatch(e -> e.marketplace().equals("MERCADO_LIVRE") || e.marketplace().equals("ALL"));
    }
}
