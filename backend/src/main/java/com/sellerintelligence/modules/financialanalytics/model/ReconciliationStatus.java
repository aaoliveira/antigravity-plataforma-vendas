package com.sellerintelligence.modules.financialanalytics.model;

/**
 * Estado da reconciliação entre valores estimados e extrato externo de liquidação.
 */
public enum ReconciliationStatus {
    /**
     * Valores apurados por estimativa de regras vigentes; liquidação externa ainda pendente.
     */
    ESTIMATED,

    /**
     * Reconciliado com sucesso contra liquidação externa; diferenças centesimais dentro da tolerância.
     */
    RECONCILED,

    /**
     * Divergência financeira detectada entre valores estimados e os valores efetivamente debitados pelo canal.
     */
    DIVERGENT,

    /**
     * Liquidação financeira externa recebida parcialmente.
     */
    PARTIAL
}
