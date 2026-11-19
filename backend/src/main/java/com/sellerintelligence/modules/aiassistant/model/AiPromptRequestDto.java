package com.sellerintelligence.modules.aiassistant.model;

import com.sellerintelligence.modules.aiassistant.gateway.AiProvider;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * Requisição tipada para execução assistiva de IA sob guardrails de governança.
 */
public record AiPromptRequestDto(
        @NotBlank(message = "promptKey é obrigatório")
        String promptKey,

        String promptVersion,

        AiProvider provider,

        String model,

        String userInput,

        Map<String, Object> entityContext,

        Double temperature
) {
}
