package com.sellerintelligence.modules.listinganalytics.model;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Catálogo central documentado de métricas oficiais disponibilizadas por marketplace.
 * Governa com rigor as capacidades reais dos canais e proíbe estimativas silenciosas.
 */
@Component
public class OfficialMetricCatalog {

    private final List<MetricCatalogEntry> entries;

    public OfficialMetricCatalog() {
        List<MetricCatalogEntry> list = new ArrayList<>();

        // =========================================================================
        // 1. MERCADO LIVRE (Conta Conectada e Anúncios Próprios)
        // =========================================================================
        list.add(new MetricCatalogEntry(
                "VISITS",
                "Visitas de Anúncio / Conta",
                "Visitas únicas e sessões de compradores recebidas pelo anúncio ou conta.",
                "MERCADO_LIVRE",
                MetricScope.LISTING,
                MetricGranularity.DAILY,
                MetricUnit.COUNT,
                true,
                "/items/{id}/visits/time_window e /users/{id}/items_visits",
                "100 req/min por conta conectada",
                365,
                null,
                null,
                null
        ));

        list.add(new MetricCatalogEntry(
                "ORDERS_COUNT",
                "Pedidos Confirmados",
                "Quantidade de pedidos de venda fechados com sucesso no canal.",
                "MERCADO_LIVRE",
                MetricScope.LISTING,
                MetricGranularity.DAILY,
                MetricUnit.COUNT,
                true,
                "/orders/search",
                "20 req/s por conta conectada",
                null,
                null,
                null,
                null
        ));

        list.add(new MetricCatalogEntry(
                "UNITS_SOLD",
                "Unidades Vendidas",
                "Quantidade total de unidades físicas faturadas nos pedidos.",
                "MERCADO_LIVRE",
                MetricScope.LISTING,
                MetricGranularity.DAILY,
                MetricUnit.COUNT,
                true,
                "/orders/search",
                "20 req/s por conta conectada",
                null,
                null,
                null,
                null
        ));

        list.add(new MetricCatalogEntry(
                "REVENUE",
                "Receita Bruta Própria",
                "Valor bruto total transacionado pelas vendas da própria conta conectada.",
                "MERCADO_LIVRE",
                MetricScope.LISTING,
                MetricGranularity.DAILY,
                MetricUnit.CURRENCY,
                true,
                "/orders/search",
                "20 req/s por conta conectada",
                null,
                null,
                null,
                null
        ));

        list.add(new MetricCatalogEntry(
                "IMPRESSIONS",
                "Impressões de Busca Orgânica",
                "Quantidade de exibições do anúncio nas páginas de busca orgânica.",
                "MERCADO_LIVRE",
                MetricScope.LISTING,
                MetricGranularity.DAILY,
                MetricUnit.COUNT,
                false,
                "Inexistente na API oficial de tráfego orgânico",
                "0 req/s",
                null,
                null,
                null,
                "A API oficial do Mercado Livre não disponibiliza impressões de busca orgânica para vendedores. Disponível exclusivamente sob contratação de Mercado Ads (Product Ads API)."
        ));

        list.add(new MetricCatalogEntry(
                "CLICKS",
                "Cliques de Busca Orgânica",
                "Cliques orgânicos recebidos na listagem de resultados de pesquisa.",
                "MERCADO_LIVRE",
                MetricScope.LISTING,
                MetricGranularity.DAILY,
                MetricUnit.COUNT,
                false,
                "Inexistente na API oficial de tráfego orgânico",
                "0 req/s",
                null,
                null,
                null,
                "A API oficial do Mercado Livre não disponibiliza contagem de cliques orgânicos isolada de visitas. Disponível exclusivamente via Mercado Ads."
        ));

        list.add(new MetricCatalogEntry(
                "CTR",
                "Taxa de Cliques (CTR)",
                "Razão entre cliques e impressões recebidas (clicks / impressions).",
                "MERCADO_LIVRE",
                MetricScope.LISTING,
                MetricGranularity.DAILY,
                MetricUnit.PERCENTAGE,
                false,
                "Derivada: clicks / impressions * 100",
                "Computada localmente",
                null,
                "clicks / impressions * 100",
                "IMPRESSIONS",
                "Indisponível no tráfego orgânico do Mercado Livre devido à ausência oficial de dados de impressões. Aplicável somente sob contratação de campanhas de publicidade do Mercado Ads."
        ));

        list.add(new MetricCatalogEntry(
                "CONVERSION_RATE_ORDERS",
                "Taxa de Conversão por Pedidos",
                "Percentual de visitas que resultaram em pedidos confirmados (orders_count / visits).",
                "MERCADO_LIVRE",
                MetricScope.LISTING,
                MetricGranularity.DAILY,
                MetricUnit.PERCENTAGE,
                true,
                "Derivada: /orders/search dividido por /items/{id}/visits",
                "Computada localmente",
                365,
                "orders_count / visits * 100",
                "VISITS",
                null
        ));

        list.add(new MetricCatalogEntry(
                "CONVERSION_RATE_UNITS",
                "Taxa de Conversão por Unidades",
                "Razão média de unidades vendidas por visita recebida (units_sold / visits).",
                "MERCADO_LIVRE",
                MetricScope.LISTING,
                MetricGranularity.DAILY,
                MetricUnit.PERCENTAGE,
                true,
                "Derivada: /orders/search dividido por /items/{id}/visits",
                "Computada localmente",
                365,
                "units_sold / visits * 100",
                "VISITS",
                null
        ));

        // =========================================================================
        // 2. SHOPEE (Open Platform v2 - Loja Conectada e Anúncios Próprios)
        // =========================================================================
        list.add(new MetricCatalogEntry(
                "PAGE_VIEWS",
                "Visualizações de Página (Page Views)",
                "Total de exibições da página do anúncio ou da loja.",
                "SHOPEE",
                MetricScope.LISTING,
                MetricGranularity.DAILY,
                MetricUnit.COUNT,
                true,
                "v2.analytics.get_item_performance e v2.analytics.get_shop_performance",
                "10 req/s",
                90,
                null,
                null,
                null
        ));

        list.add(new MetricCatalogEntry(
                "UNIQUE_VISITORS",
                "Visitantes Únicos",
                "Quantidade de compradores distintos que visualizaram o anúncio.",
                "SHOPEE",
                MetricScope.LISTING,
                MetricGranularity.DAILY,
                MetricUnit.COUNT,
                true,
                "v2.analytics.get_item_performance",
                "10 req/s",
                90,
                null,
                null,
                null
        ));

        list.add(new MetricCatalogEntry(
                "ORDERS_COUNT",
                "Pedidos Confirmados",
                "Total de pedidos de venda fechados com sucesso na loja.",
                "SHOPEE",
                MetricScope.LISTING,
                MetricGranularity.DAILY,
                MetricUnit.COUNT,
                true,
                "v2.order.get_order_list",
                "10 req/s",
                null,
                null,
                null,
                null
        ));

        list.add(new MetricCatalogEntry(
                "UNITS_SOLD",
                "Unidades Vendidas",
                "Quantidade física de itens comercializados nos pedidos.",
                "SHOPEE",
                MetricScope.LISTING,
                MetricGranularity.DAILY,
                MetricUnit.COUNT,
                true,
                "v2.order.get_order_list",
                "10 req/s",
                null,
                null,
                null,
                null
        ));

        list.add(new MetricCatalogEntry(
                "REVENUE",
                "GMV / Receita Bruta Própria",
                "Valor monetário transacionado nas vendas da própria conta de seller.",
                "SHOPEE",
                MetricScope.LISTING,
                MetricGranularity.DAILY,
                MetricUnit.CURRENCY,
                true,
                "v2.order.get_order_list",
                "10 req/s",
                null,
                null,
                null,
                null
        ));

        list.add(new MetricCatalogEntry(
                "IMPRESSIONS",
                "Impressões de Publicidade (Shopee Ads)",
                "Exibições de anúncios patrocinados da loja em resultados de busca.",
                "SHOPEE",
                MetricScope.LISTING,
                MetricGranularity.DAILY,
                MetricUnit.COUNT,
                true,
                "v2.ads.get_item_performance",
                "10 req/s",
                90,
                null,
                null,
                null
        ));

        list.add(new MetricCatalogEntry(
                "CLICKS",
                "Cliques de Publicidade (Shopee Ads)",
                "Cliques efetuados por compradores em anúncios patrocinados da loja.",
                "SHOPEE",
                MetricScope.LISTING,
                MetricGranularity.DAILY,
                MetricUnit.COUNT,
                true,
                "v2.ads.get_item_performance",
                "10 req/s",
                90,
                null,
                null,
                null
        ));

        list.add(new MetricCatalogEntry(
                "CTR",
                "Taxa de Cliques (CTR Shopee Ads)",
                "Razão entre cliques e impressões nas campanhas de publicidade patrocinadas.",
                "SHOPEE",
                MetricScope.LISTING,
                MetricGranularity.DAILY,
                MetricUnit.PERCENTAGE,
                true,
                "Derivada: v2.ads clicks / impressions * 100",
                "Computada localmente",
                90,
                "clicks / impressions * 100",
                "IMPRESSIONS",
                null
        ));

        list.add(new MetricCatalogEntry(
                "CONVERSION_RATE_ORDERS",
                "Taxa de Conversão por Pedidos",
                "Razão entre pedidos confirmados e visitantes únicos (orders_count / unique_visitors).",
                "SHOPEE",
                MetricScope.LISTING,
                MetricGranularity.DAILY,
                MetricUnit.PERCENTAGE,
                true,
                "Derivada: v2.order dividido por v2.analytics",
                "Computada localmente",
                90,
                "orders_count / unique_visitors * 100",
                "UNIQUE_VISITORS",
                null
        ));

        list.add(new MetricCatalogEntry(
                "CONVERSION_RATE_UNITS",
                "Taxa de Conversão por Unidades",
                "Razão entre unidades vendidas e visitantes únicos (units_sold / unique_visitors).",
                "SHOPEE",
                MetricScope.LISTING,
                MetricGranularity.DAILY,
                MetricUnit.PERCENTAGE,
                true,
                "Derivada: v2.order dividido por v2.analytics",
                "Computada localmente",
                90,
                "units_sold / unique_visitors * 100",
                "UNIQUE_VISITORS",
                null
        ));

        // =========================================================================
        // 3. MÉTRICAS CONCORRENCIAIS DE TERCEIROS (Proibidas e Não Fornecidas)
        // =========================================================================
        list.add(new MetricCatalogEntry(
                "COMPETITOR_SALES_VOLUME",
                "Volume de Vendas de Concorrentes",
                "Vendas unitárias ou faturamento de anúncios concorrentes de terceiros.",
                "ALL",
                MetricScope.LISTING,
                MetricGranularity.PERIOD_TOTAL,
                MetricUnit.COUNT,
                false,
                "Nenhum endpoint oficial ou público",
                "0 req/s",
                null,
                null,
                null,
                "Nenhum marketplace homologado disponibiliza vendas ou faturamento privados de concorrentes via API oficial. Scraping e estimativas sintéticas são categoricamente proibidos pelas políticas da plataforma e termos de serviço dos canais."
        ));

        list.add(new MetricCatalogEntry(
                "COMPETITOR_CTR",
                "CTR de Concorrentes",
                "Taxa de cliques em anúncios de terceiros.",
                "ALL",
                MetricScope.LISTING,
                MetricGranularity.PERIOD_TOTAL,
                MetricUnit.PERCENTAGE,
                false,
                "Nenhum endpoint oficial ou público",
                "0 req/s",
                null,
                null,
                null,
                "Dados de impressões e cliques de concorrentes são privados de cada conta de anunciante e não são fornecidos por APIs oficiais."
        ));

        this.entries = Collections.unmodifiableList(list);
    }

    public List<MetricCatalogEntry> getAllEntries() {
        return entries;
    }

    public List<MetricCatalogEntry> findByMarketplace(String marketplace) {
        if (marketplace == null || marketplace.isBlank()) {
            return entries;
        }
        return entries.stream()
                .filter(e -> e.marketplace().equalsIgnoreCase(marketplace) || e.marketplace().equalsIgnoreCase("ALL"))
                .toList();
    }

    public Optional<MetricCatalogEntry> findByMarketplaceAndKey(String marketplace, String metricKey) {
        if (metricKey == null || metricKey.isBlank()) {
            return Optional.empty();
        }
        return entries.stream()
                .filter(e -> e.metricKey().equalsIgnoreCase(metricKey))
                .filter(e -> marketplace == null || e.marketplace().equalsIgnoreCase(marketplace) || e.marketplace().equalsIgnoreCase("ALL"))
                .findFirst();
    }
}
