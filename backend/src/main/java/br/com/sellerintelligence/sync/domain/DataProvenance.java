package br.com.sellerintelligence.sync.domain;

/**
 * Classificação rigorosa da proveniência do dado conforme PRD e regras anti-alucinação.
 */
public enum DataProvenance {
    IMPORTED,       // Dados próprios do seller importados via API oficial autenticada ou CSV
    OBSERVED,       // Dados públicos observáveis coletados sem quebra de controle
    ESTIMATED,      // Estimativas heurísticas e cálculos derivados
    AI_GENERATED,   // Conteúdo gerado ou assistido por IA
    UNAVAILABLE     // Dado ausente ou não fornecido pela API (nunca vira zero)
}
