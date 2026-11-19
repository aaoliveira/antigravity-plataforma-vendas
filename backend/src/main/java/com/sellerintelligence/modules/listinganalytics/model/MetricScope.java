package com.sellerintelligence.modules.listinganalytics.model;

/**
 * Escopo de aplicação da métrica de desempenho.
 */
public enum MetricScope {
    /**
     * Desempenho consolidado no nível de conta / loja do vendedor.
     */
    ACCOUNT,

    /**
     * Desempenho no nível de anúncio / listing individual.
     */
    LISTING
}
