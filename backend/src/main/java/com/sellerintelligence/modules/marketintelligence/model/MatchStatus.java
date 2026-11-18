package com.sellerintelligence.modules.marketintelligence.model;

/**
 * Status do matching entre observação de mercado e produto canônico.
 * Conforme critério de aceite: "matching não une automaticamente itens abaixo do limiar".
 * - MATCHED: Score >= limiar (ex: 0.75). Unido com alta confiabilidade explicável.
 * - POTENTIAL_MATCH: Score < limiar mas >= limiar de potencial (ex: >= 0.50). Candidato para revisão manual, JAMAIS unido automaticamente.
 * - NO_MATCH: Score < 0.50. Sem correspondência relevante.
 */
public enum MatchStatus {
    MATCHED,
    POTENTIAL_MATCH,
    NO_MATCH
}
