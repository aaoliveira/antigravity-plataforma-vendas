package com.sellerintelligence.modules.financialanalytics.model;

/**
 * Proveniência do valor financeiro na apuração.
 * Diferencia formalmente valores confirmados por liquidação, estimados por regras,
 * informados pelo usuário ou ausentes (Anti-Alucinação).
 */
public enum FinancialValueProvenance {
    /**
     * Confirmado formalmente por extrato financeiro ou liquidação externa do canal.
     */
    CONFIRMED,

    /**
     * Estimado deterministamente com base nas regras vigentes de comissão e frete.
     */
    ESTIMATED,

    /**
     * Declarado diretamente pelo usuário no cadastro de custos unitários e alíquotas fiscais.
     */
    USER_INFORMED,

    /**
     * Ausente ou não parametrizado pelo usuário/canal.
     */
    MISSING
}
