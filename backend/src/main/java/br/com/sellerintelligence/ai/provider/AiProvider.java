package br.com.sellerintelligence.ai.provider;

public interface AiProvider {

    ListingAiOutput generateListingContent(
            String promptType,
            String systemPrompt,
            String userContextXml,
            String modelOverride
    );

    String getProviderName();
}
