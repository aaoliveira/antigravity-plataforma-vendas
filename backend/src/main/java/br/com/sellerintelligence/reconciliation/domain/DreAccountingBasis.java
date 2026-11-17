package br.com.sellerintelligence.reconciliation.domain;

public enum DreAccountingBasis {
    ACCRUAL, // Regime de Competência (Data do Pedido / Fato Gerador)
    CASH     // Regime de Caixa (Data da Liquidação / Repasse Efetivo)
}
