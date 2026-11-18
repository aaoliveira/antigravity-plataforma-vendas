package com.sellerintelligence.modules.marketplaceconnection.mercadolivre;

/**
 * Representação imutável dos parâmetros PKCE (RFC 7636).
 */
public record PkceChallenge(
        String codeVerifier,
        String codeChallenge,
        String codeChallengeMethod
) {
    public PkceChallenge {
        if (codeVerifier == null || codeVerifier.isBlank()) {
            throw new IllegalArgumentException("codeVerifier não pode ser vazio");
        }
        if (codeChallenge == null || codeChallenge.isBlank()) {
            throw new IllegalArgumentException("codeChallenge não pode ser vazio");
        }
        if (!"S256".equals(codeChallengeMethod)) {
            throw new IllegalArgumentException("Apenas o método S256 é suportado para PKCE");
        }
    }
}
