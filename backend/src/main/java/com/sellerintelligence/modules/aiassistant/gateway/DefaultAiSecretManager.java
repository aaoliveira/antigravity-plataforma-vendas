package com.sellerintelligence.modules.aiassistant.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * Implementação padrão de Secret Manager para chaves de IA com mascaramento estrito.
 * Proibido registrar chaves em texto claro nos logs em qualquer nível.
 */
@Component
public class DefaultAiSecretManager implements AiSecretManager {

    private static final Logger log = LoggerFactory.getLogger(DefaultAiSecretManager.class);

    private final Map<AiProvider, String> providerKeys = new EnumMap<>(AiProvider.class);

    public DefaultAiSecretManager(
            @Value("${ai.provider.gemini.api-key:}") String geminiKey,
            @Value("${ai.provider.openai.api-key:}") String openaiKey,
            @Value("${ai.provider.claude.api-key:}") String claudeKey
    ) {
        if (geminiKey != null && !geminiKey.isBlank()) {
            providerKeys.put(AiProvider.GEMINI, geminiKey.trim());
        }
        if (openaiKey != null && !openaiKey.isBlank()) {
            providerKeys.put(AiProvider.OPENAI, openaiKey.trim());
        }
        if (claudeKey != null && !claudeKey.isBlank()) {
            providerKeys.put(AiProvider.CLAUDE, claudeKey.trim());
        }
        // Mock Provider sempre disponível para ambientes de teste e fallback determinístico
        providerKeys.put(AiProvider.MOCK_PROVIDER, "mock-key-testing-internal-only");

        log.info("AiSecretManager inicializado com credenciais para: {}",
                providerKeys.keySet().stream().map(Enum::name).toList());
    }

    /**
     * Permite registrar ou rotacionar credencial dinamicamente em runtime.
     */
    public void registerKey(AiProvider provider, String apiKey) {
        if (apiKey != null && !apiKey.isBlank()) {
            providerKeys.put(provider, apiKey.trim());
            log.info("Credencial rotacionada com sucesso para provedor {}: {}",
                    provider, maskSecret(apiKey));
        }
    }

    @Override
    public String getApiKey(AiProvider provider) {
        String key = providerKeys.get(provider);
        if (key == null || key.isBlank()) {
            throw new IllegalStateException(
                    "Credencial não configurada para o provedor de IA: " + provider +
                    ". Verifique o Secret Manager ou variáveis de ambiente.");
        }
        return key;
    }

    @Override
    public boolean hasCredentials(AiProvider provider) {
        String key = providerKeys.get(provider);
        return key != null && !key.isBlank();
    }

    @Override
    public String maskSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            return "[EMPTY]";
        }
        String trimmed = secret.trim();
        if (trimmed.length() <= 8) {
            return "sk-***" + trimmed.charAt(trimmed.length() - 1);
        }
        String prefix = trimmed.startsWith("sk-") ? "sk-***" : "***";
        String suffix = trimmed.substring(trimmed.length() - 4);
        return prefix + suffix;
    }
}
