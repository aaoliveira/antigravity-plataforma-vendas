package com.sellerintelligence.modules.marketplaceconnection.mercadolivre;

import com.sellerintelligence.modules.marketplaceconnection.exception.InvalidStateCsrfException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuthStateManagerTest {

    private OAuthStateManager stateManager;

    @BeforeEach
    void setUp() {
        stateManager = new OAuthStateManager();
    }

    @Test
    @DisplayName("Gera e valida com sucesso o state anti-CSRF associado ao tenant e code_verifier")
    void testGenerateAndConsumeStateSuccess() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String codeVerifier = "test-pkce-verifier-1234567890-abcdefghij-klmnopqrstuv";

        OAuthState state = stateManager.generateAndStoreState(tenantId, userId, codeVerifier);

        assertThat(state).isNotNull();
        assertThat(state.stateToken()).isNotBlank();
        assertThat(state.tenantId()).isEqualTo(tenantId);
        assertThat(state.userId()).isEqualTo(userId);
        assertThat(state.codeVerifier()).isEqualTo(codeVerifier);

        // Consome atomicamente
        OAuthState consumed = stateManager.consumeAndValidateState(state.stateToken(), tenantId);

        assertThat(consumed).isNotNull();
        assertThat(consumed.codeVerifier()).isEqualTo(codeVerifier);

        // Segunda tentativa deve ser rejeitada (One-Time Use)
        assertThatThrownBy(() -> stateManager.consumeAndValidateState(state.stateToken(), tenantId))
                .isInstanceOf(InvalidStateCsrfException.class)
                .hasMessageContaining("já consumido ou inexistente");
    }

    @Test
    @DisplayName("Rejeita state quando o tenant informado for diferente do tenant criador")
    void testRejectStateCrossTenant() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        OAuthState state = stateManager.generateAndStoreState(tenantA, userId, "verifier");

        assertThatThrownBy(() -> stateManager.consumeAndValidateState(state.stateToken(), tenantB))
                .isInstanceOf(InvalidStateCsrfException.class)
                .hasMessageContaining("não corresponde ao tenant ativo");
    }

    @Test
    @DisplayName("Rejeita state nulo ou vazio")
    void testRejectNullOrEmptyState() {
        UUID tenant = UUID.randomUUID();

        assertThatThrownBy(() -> stateManager.consumeAndValidateState(null, tenant))
                .isInstanceOf(InvalidStateCsrfException.class);

        assertThatThrownBy(() -> stateManager.consumeAndValidateState("   ", tenant))
                .isInstanceOf(InvalidStateCsrfException.class);
    }
}
