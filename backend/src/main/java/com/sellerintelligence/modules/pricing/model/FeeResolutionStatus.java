package com.sellerintelligence.modules.pricing.model;

/**
 * Status de completude da resolução de taxas para um anúncio/cenário.
 */
public enum FeeResolutionStatus {
    /**
     * Todas as taxas obrigatórias (ex: comissão) foram encontradas e calculadas com fontes válidas.
     */
    COMPLETE,

    /**
     * Algumas taxas foram encontradas (ex: taxa fixa), mas falta comissão ou componente essencial.
     */
    PARTIAL,

    /**
     * Nenhuma regra de taxa foi cadastrada para o marketplace/modalidade informado.
     * A plataforma não assume 0 e solicita a entrada do usuário.
     */
    MISSING
}
