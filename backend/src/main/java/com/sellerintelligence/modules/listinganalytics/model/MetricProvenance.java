package com.sellerintelligence.modules.listinganalytics.model;

/**
 * Proveniência da métrica registrada no sistema.
 */
public enum MetricProvenance {
    /**
     * Métrica coletada diretamente de API oficial homologada do marketplace.
     */
    OFFICIAL_API,

    /**
     * Métrica calculada deterministamente através de fórmula documentada a partir de métricas oficiais.
     */
    DERIVED_FORMULA,

    /**
     * Indicador não disponível na fonte.
     */
    UNAVAILABLE
}
