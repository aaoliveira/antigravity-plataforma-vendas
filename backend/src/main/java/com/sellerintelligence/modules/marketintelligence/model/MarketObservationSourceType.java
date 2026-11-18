package com.sellerintelligence.modules.marketintelligence.model;

/**
 * Classificação obrigatória de proveniência de dados da observação de mercado.
 * Conforme regras do PRD e anti-alucinação:
 * - OFFICIAL: Proveniente de integração oficial autorizada da conta do próprio vendedor.
 * - OBSERVED: Dado público ou direto coletado legitimamente sem transformação heurística.
 * - ESTIMATED: Dado inferido através de metodologia estatística documentada e versionada.
 * - USER_INPUT: Informado manualmente pelo usuário para simulação ou análise.
 */
public enum MarketObservationSourceType {
    OFFICIAL,
    OBSERVED,
    ESTIMATED,
    USER_INPUT
}
