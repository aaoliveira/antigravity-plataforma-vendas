package com.sellerintelligence.modules.aiassistant.model;

import java.util.Map;

/**
 * DTO descritivo para catálogo público de tools somente leitura expostas.
 */
public record AiToolDefinitionDto(
        String name,
        String description,
        Map<String, Object> parametersSchema
) {
}
