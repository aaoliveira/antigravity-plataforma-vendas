package com.sellerintelligence.modules.listinganalytics.model;

/**
 * Unidade de medida da métrica.
 */
public enum MetricUnit {
    /**
     * Quantidade inteira contável (ex: visitas, pedidos, cliques).
     */
    COUNT,

    /**
     * Valor monetário (ex: receita bruta, GMV).
     */
    CURRENCY,

    /**
     * Taxa percentual normalizada (0% a 100%, ex: 2.50%).
     */
    PERCENTAGE,

    /**
     * Razão matemática decimal pura (0.000000 a 1.000000).
     */
    RATIO
}
