package com.sellerintelligence.modules.marketintelligence.model;

import java.util.UUID;

/**
 * Resposta de candidato a match retornado pela análise de inteligência de mercado.
 */
public record ProductMatchCandidateResponse(
        UUID canonicalProductId,
        String productTitle,
        String canonicalSku,
        String brand,
        MatchScoreBreakdown scoreBreakdown
) {
}
