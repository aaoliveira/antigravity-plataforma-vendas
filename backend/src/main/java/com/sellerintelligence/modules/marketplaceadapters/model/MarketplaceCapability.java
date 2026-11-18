package com.sellerintelligence.modules.marketplaceadapters.model;

/**
 * Capacidades funcionais catalogadas para integração com marketplaces.
 */
public enum MarketplaceCapability {

    /**
     * Autenticação oficial (OAuth 2.0 PKCE / HMAC-SHA256).
     */
    OFFICIAL_AUTH,

    /**
     * Leitura de catálogo de anúncios e inventário do próprio seller.
     */
    READ_OWN_CATALOG,

    /**
     * Leitura de pedidos próprios e detalhes fiscais/financeiros do pedido.
     */
    READ_OWN_ORDERS,

    /**
     * Pesquisa pública de mercado em fontes permitidas para descoberta de produtos e tendências.
     */
    MARKET_SEARCH,

    /**
     * Snapshots temporais de preços observados em anúncios cadastrados para monitoramento.
     */
    OBSERVED_PRICE_SNAPSHOT,

    /**
     * Métricas oficiais de anúncios (visitas, conversão, health score oficial do canal).
     */
    OFFICIAL_LISTING_METRICS,

    /**
     * Volume de vendas e faturamento de concorrentes (proibido forjar/estimar como real).
     */
    COMPETITOR_SALES_VOLUME,

    /**
     * Operações de escrita em canais (alterar preços, publicar anúncios, impulsionar) - Desabilitadas no MVP.
     */
    WRITE_OPERATIONS
}
