package com.sellerintelligence.modules.aiassistant.gateway;

/**
 * Contrato para gestão segura de credenciais de provedores de IA.
 * Garante proteção fail-closed e mascaramento permanente de segredos.
 */
public interface AiSecretManager {

    /**
     * Retorna a chave de API para o provedor solicitado.
     * Lança exceção se não configurada em produção.
     */
    String getApiKey(AiProvider provider);

    /**
     * Verifica se o provedor possui credenciais válidas configuradas.
     */
    boolean hasCredentials(AiProvider provider);

    /**
     * Mascara o segredo para exibição segura em logs ou auditorias (ex: sk-***...12ab).
     * Jamais retorna o segredo em texto claro.
     */
    String maskSecret(String secret);
}
