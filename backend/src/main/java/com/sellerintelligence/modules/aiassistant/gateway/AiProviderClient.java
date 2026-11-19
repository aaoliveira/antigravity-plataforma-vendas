package com.sellerintelligence.modules.aiassistant.gateway;

import com.sellerintelligence.modules.aiassistant.prompts.PromptTemplate;
import com.sellerintelligence.modules.aiassistant.tools.AiToolRegistry;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Contrato de comunicação multi-provider para geração de conteúdo e orquestração de tools.
 */
public interface AiProviderClient {

    record ProviderExecutionResult(
            String textContent,
            Object structuredData,
            List<String> toolsExecuted,
            List<String> sources,
            List<String> unavailabilityNotes,
            int promptTokens,
            int completionTokens,
            long latencyMs
    ) {}

    ProviderExecutionResult execute(
            AiProvider provider,
            String model,
            PromptTemplate template,
            String sanitizedUserInput,
            Map<String, Object> entityContext,
            AiToolRegistry toolRegistry,
            UUID authenticatedTenantId,
            AiModelConfig config
    );
}
