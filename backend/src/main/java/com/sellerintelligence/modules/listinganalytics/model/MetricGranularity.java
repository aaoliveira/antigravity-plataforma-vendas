package com.sellerintelligence.modules.listinganalytics.model;

/**
 * Granularidade temporal da métrica.
 */
public enum MetricGranularity {
    /**
     * Métrica agregada por dia calendário (00:00 - 23:59:59).
     */
    DAILY,

    /**
     * Métrica acumulada consolidada em toda a janela temporal solicitada.
     */
    PERIOD_TOTAL
}
