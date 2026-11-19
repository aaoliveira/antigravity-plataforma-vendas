package com.sellerintelligence.modules.aiassistant.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultAiSecretManagerTest {

    @Test
    @DisplayName("Mascaramento de chave de API nunca expõe o corpo do segredo")
    void shouldMaskApiKeyCorrectly() {
        DefaultAiSecretManager manager = new DefaultAiSecretManager("", "", "");

        assertThat(manager.maskSecret("sk-proj-1234567890abcdef1234")).isEqualTo("sk-***1234");
        assertThat(manager.maskSecret("AIzaSyBv998877665544332211")).isEqualTo("***2211");
        assertThat(manager.maskSecret("short")).isEqualTo("sk-***t");
        assertThat(manager.maskSecret("")).isEqualTo("[EMPTY]");
        assertThat(manager.maskSecret(null)).isEqualTo("[EMPTY]");
    }

    @Test
    @DisplayName("Mock Provider sempre possui credenciais ativas para ambiente hermético")
    void shouldHaveCredentialsForMockProvider() {
        DefaultAiSecretManager manager = new DefaultAiSecretManager("", "", "");

        assertThat(manager.hasCredentials(AiProvider.MOCK_PROVIDER)).isTrue();
        assertThat(manager.getApiKey(AiProvider.MOCK_PROVIDER)).isEqualTo("mock-key-testing-internal-only");
    }

    @Test
    @DisplayName("Provedor sem chave configurada reporta ausência e lança IllegalStateException ao tentar obter chave (Fail-Closed)")
    void shouldFailClosedWhenCredentialsMissing() {
        DefaultAiSecretManager manager = new DefaultAiSecretManager("", "", "");

        assertThat(manager.hasCredentials(AiProvider.OPENAI)).isFalse();
        assertThatThrownBy(() -> manager.getApiKey(AiProvider.OPENAI))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Credencial não configurada para o provedor de IA: OPENAI");
    }

    @Test
    @DisplayName("Permite injeção e rotação dinâmica de credenciais em tempo de execução")
    void shouldSupportDynamicKeyRotation() {
        DefaultAiSecretManager manager = new DefaultAiSecretManager("", "", "");

        assertThat(manager.hasCredentials(AiProvider.GEMINI)).isFalse();

        manager.registerKey(AiProvider.GEMINI, "AIzaSyTestKeyRotate1234");

        assertThat(manager.hasCredentials(AiProvider.GEMINI)).isTrue();
        assertThat(manager.getApiKey(AiProvider.GEMINI)).isEqualTo("AIzaSyTestKeyRotate1234");
        assertThat(manager.maskSecret(manager.getApiKey(AiProvider.GEMINI))).isEqualTo("***1234");
    }
}
