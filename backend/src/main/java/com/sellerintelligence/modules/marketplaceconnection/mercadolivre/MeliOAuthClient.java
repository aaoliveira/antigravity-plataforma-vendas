package com.sellerintelligence.modules.marketplaceconnection.mercadolivre;

/**
 * Contrato para comunicação OAuth com a API do Mercado Livre.
 */
public interface MeliOAuthClient {

    /**
     * Monta a URL oficial de redirecionamento para autorização pelo lojista.
     *
     * @param state token anti-CSRF
     * @param codeChallenge code_challenge gerado para PKCE
     * @return URL completa de autorização do Mercado Livre
     */
    String buildAuthorizationUrl(String state, String codeChallenge);

    /**
     * Efetua a troca do authorization code temporário por tokens definitivos.
     *
     * @param code authorization code temporário recebido no callback
     * @param codeVerifier segredo verifier do PKCE
     * @param redirectUri URI de redirecionamento previamente cadastrada
     * @return resposta de tokens oficial do canal
     */
    MeliTokenResponse exchangeAuthorizationCode(String code, String codeVerifier, String redirectUri);

    /**
     * Executa a renovação segura de access token utilizando o refresh token corrente.
     *
     * @param refreshToken refresh token ativo
     * @return novo par de tokens (access_token e novo refresh_token rotacionado)
     */
    MeliTokenResponse refreshAccessToken(String refreshToken);
}
