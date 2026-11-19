package com.sellerintelligence.modules.listinganalytics.model;

/**
 * Status formal de disponibilidade da métrica.
 * Previne categoricamente estimativas silenciosas ou substituição por zeros.
 */
public enum MetricAvailabilityStatus {
    /**
     * Métrica fornecida oficialmente pela API do marketplace ou derivada validamente.
     */
    AVAILABLE,

    /**
     * Métrica ausente ou temporariamente indisponível no canal para o período.
     */
    UNAVAILABLE,

    /**
     * Denominador zero ou amostra de dados insuficiente para o cálculo da razão/taxa.
     */
    INSUFFICIENT_DATA,

    /**
     * O marketplace não disponibiliza este indicador oficialmente no contexto (ex: impressões orgânicas ML).
     */
    UNSUPPORTED_BY_CHANNEL
}
