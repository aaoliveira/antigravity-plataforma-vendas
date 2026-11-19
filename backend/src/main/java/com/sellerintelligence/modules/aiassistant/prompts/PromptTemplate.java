package com.sellerintelligence.modules.aiassistant.prompts;

import java.time.Instant;
import java.util.List;

/**
 * Representação imutável de um System Prompt versionado e auditado.
 */
public record PromptTemplate(
        String key,
        String version,
        String systemInstructions,
        List<String> allowedTools,
        String description,
        Instant registeredAt
) {
}
