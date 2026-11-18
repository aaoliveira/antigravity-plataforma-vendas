package com.sellerintelligence.modules.marketplaceconnection.mercadolivre;

import com.sellerintelligence.modules.marketplaceconnection.exception.MeliOAuthException;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cliente Fake / Sandbox do Mercado Livre para testes e homologação local.
 * Permite simulação completa do ciclo de vida OAuth sem dependência de rede externa.
 */
public class FakeMeliOAuthClient implements MeliOAuthClient {

    private final AtomicInteger exchangeCount = new AtomicInteger(0);
    private final AtomicInteger refreshCount = new AtomicInteger(0);
    private boolean simulateFailure = false;
    private String simulatedUserId = "123456789";

    @Override
    public String buildAuthorizationUrl(String state, String codeChallenge) {
        return "https://auth.mercadolivre.com.br/authorization?response_type=code&client_id=test-app-id&redirect_uri=http://localhost:3000/callback&state="
                + state + "&code_challenge=" + codeChallenge + "&code_challenge_method=S256";
    }

    @Override
    public MeliTokenResponse exchangeAuthorizationCode(String code, String codeVerifier, String redirectUri) {
        exchangeCount.incrementAndGet();

        if (simulateFailure) {
            throw new MeliOAuthException("invalid_grant", "Código de autorização inválido ou expirado");
        }

        if (code == null || code.isBlank() || codeVerifier == null || codeVerifier.isBlank()) {
            throw new MeliOAuthException("invalid_request", "Parâmetros code e code_verifier são obrigatórios");
        }

        return new MeliTokenResponse(
                "APP_USR-mock-access-token-" + exchangeCount.get(),
                "bearer",
                21600L,
                "read offline_access",
                simulatedUserId,
                "TG-mock-refresh-token-" + exchangeCount.get()
        );
    }

    @Override
    public MeliTokenResponse refreshAccessToken(String refreshToken) {
        refreshCount.incrementAndGet();

        if (simulateFailure) {
            throw new MeliOAuthException("invalid_grant", "Refresh token revogado ou inválido");
        }

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new MeliOAuthException("invalid_request", "Refresh token obrigatório");
        }

        return new MeliTokenResponse(
                "APP_USR-refreshed-token-" + refreshCount.get(),
                "bearer",
                21600L,
                "read offline_access",
                simulatedUserId,
                "TG-refreshed-token-" + refreshCount.get()
        );
    }

    public int getExchangeCount() {
        return exchangeCount.get();
    }

    public int getRefreshCount() {
        return refreshCount.get();
    }

    public void setSimulateFailure(boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }

    public void setSimulatedUserId(String simulatedUserId) {
        this.simulatedUserId = simulatedUserId;
    }

    public void reset() {
        exchangeCount.set(0);
        refreshCount.set(0);
        simulateFailure = false;
    }
}
