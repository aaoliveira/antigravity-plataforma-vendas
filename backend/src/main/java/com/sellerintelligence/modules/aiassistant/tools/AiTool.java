package com.sellerintelligence.modules.aiassistant.tools;

import java.util.Map;
import java.util.UUID;

/**
 * Interface de ferramenta declarativa interna exposta para invocação segura pelo LLM.
 * Toda ferramenta é ESTRITAMENTE somente leitura e isolada por tenant_id.
 */
public interface AiTool {

    String getName();

    String getDescription();

    Map<String, Object> getParametersSchema();

    ToolExecutionResult execute(UUID authenticatedTenantId, Map<String, Object> arguments);
}
