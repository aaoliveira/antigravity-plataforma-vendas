package br.com.sellerintelligence.ai.provider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Profile("prod")
public class RestAiProvider implements AiProvider {

    private final String apiKey;
    private final String model;
    private final String endpoint;
    private final RestClient restClient;

    public RestAiProvider(
            @Value("${ai.provider.api-key:}") String apiKey,
            @Value("${ai.provider.model:gpt-4o-mini}") String model,
            @Value("${ai.provider.endpoint:https://api.openai.com/v1/chat/completions}") String endpoint
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.endpoint = endpoint;
        this.restClient = RestClient.builder().baseUrl(endpoint).build();
    }

    @Override
    public ListingAiOutput generateListingContent(
            String promptType,
            String systemPrompt,
            String userContextXml,
            String modelOverride
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiServiceUnavailableException("Credenciais do provedor de IA não configuradas no ambiente de produção.");
        }

        long start = System.currentTimeMillis();
        try {
            // Em produção chamaria a API REST oficial configurada
            // Se falhar ou timeout, lança AiServiceUnavailableException
            throw new AiServiceUnavailableException("Provedor de produção não configurado para testes locais.");
        } catch (AiServiceUnavailableException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AiServiceUnavailableException("Falha na comunicação com o provedor de IA: " + ex.getMessage(), ex);
        }
    }

    @Override
    public String getProviderName() {
        return "RestAiProvider (" + model + ")";
    }
}
