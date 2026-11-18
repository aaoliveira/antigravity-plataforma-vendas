package com.sellerintelligence.modules.marketplaceconnection.mercadolivre;

import com.sellerintelligence.modules.marketplaceconnection.exception.InvalidStateCsrfException;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador de ciclo de vida de tokens de state anti-CSRF para o fluxo OAuth 2.0.
 * Garante expiração estrita (TTL de 10 minutos) e consumo atômico de uso único (one-time use).
 */
@Component
public class OAuthStateManager {

    private static final Duration STATE_TTL = Duration.ofMinutes(10);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final Map<String, OAuthState> activeStates = new ConcurrentHashMap<>();

    /**
     * Cria e armazena um novo state anti-CSRF associado ao tenant, usuário e code_verifier PKCE.
     */
    public OAuthState generateAndStoreState(UUID tenantId, UUID userId, String codeVerifier) {
        cleanExpiredStates();

        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String stateToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        Instant now = Instant.now();
        Instant expiresAt = now.plus(STATE_TTL);

        OAuthState state = new OAuthState(stateToken, tenantId, userId, codeVerifier, now, expiresAt);
        activeStates.put(stateToken, state);

        return state;
    }

    /**
     * Valida e consome atomicamente o state anti-CSRF.
     * Após ser consumido, o state é imediatamente removido da memória, impedindo ataques de repetição.
     *
     * @param stateToken token de state recebido no callback do Mercado Livre
     * @param expectedTenantId tenant_id do contexto da requisição
     * @return o estado consumido contendo o code_verifier original
     * @throws InvalidStateCsrfException se o state não existir, estiver expirado ou pertencer a outro tenant
     */
    public OAuthState consumeAndValidateState(String stateToken, UUID expectedTenantId) {
        if (stateToken == null || stateToken.isBlank()) {
            throw new InvalidStateCsrfException("Parâmetro 'state' anti-CSRF é obrigatório e não foi informado");
        }

        OAuthState state = activeStates.remove(stateToken);
        if (state == null) {
            throw new InvalidStateCsrfException("State OAuth inválido, já consumido ou inexistente");
        }

        if (state.isExpired()) {
            throw new InvalidStateCsrfException("State OAuth expirado. Por favor, reinicie a autorização");
        }

        if (expectedTenantId != null && !state.tenantId().equals(expectedTenantId)) {
            throw new InvalidStateCsrfException("State OAuth não corresponde ao tenant ativo");
        }

        return state;
    }

    private void cleanExpiredStates() {
        activeStates.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}
