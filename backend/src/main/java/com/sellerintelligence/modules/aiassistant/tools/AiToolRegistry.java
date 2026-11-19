package com.sellerintelligence.modules.aiassistant.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro e validador de execução de ferramentas de IA.
 * Aplica controle de acesso Fail-Closed e bloqueio sumário de parâmetros de tenant não autorizados.
 */
@Component
public class AiToolRegistry {

    private static final Logger log = LoggerFactory.getLogger(AiToolRegistry.class);

    private final Map<String, AiTool> tools = new ConcurrentHashMap<>();

    public AiToolRegistry(InternalAiTools internalAiTools) {
        for (AiTool tool : internalAiTools.getAllTools()) {
            registerTool(tool);
        }
    }

    public void registerTool(AiTool tool) {
        tools.put(tool.getName().toLowerCase(), tool);
        log.info("Tool registrada com sucesso: '{}'", tool.getName());
    }

    public Optional<AiTool> getTool(String name) {
        if (name == null) return Optional.empty();
        return Optional.ofNullable(tools.get(name.toLowerCase()));
    }

    public Collection<AiTool> listAllTools() {
        return Collections.unmodifiableCollection(tools.values());
    }

    /**
     * Executa a ferramenta validando isolamento de tenant de forma hermética.
     */
    public ToolExecutionResult execute(
            String toolName,
            UUID authenticatedTenantId,
            List<String> allowedToolsForPrompt,
            Map<String, Object> arguments
    ) {
        if (authenticatedTenantId == null) {
            log.error("Tentativa de executar tool '{}' sem tenant_id autenticado no contexto!", toolName);
            return ToolExecutionResult.accessDenied("Sessão sem tenantId autenticado.");
        }

        AiTool tool = tools.get(toolName.toLowerCase());
        if (tool == null) {
            return ToolExecutionResult.error("Ferramenta '" + toolName + "' não encontrada no registro.");
        }

        // Valida se o prompt tem permissão para chamar esta tool
        if (allowedToolsForPrompt != null && !allowedToolsForPrompt.isEmpty()) {
            boolean isAllowed = allowedToolsForPrompt.stream()
                    .anyMatch(a -> a.equalsIgnoreCase(toolName));
            if (!isAllowed) {
                log.warn("Tool '{}' bloqueada pois não está autorizada para este prompt template!", toolName);
                return ToolExecutionResult.accessDenied("Ferramenta '" + toolName + "' não autorizada para o prompt atual.");
            }
        }

        // Defesa Hermética Multi-Tenant:
        // O LLM não pode passar argumentos tentando direcionar a consulta para outro tenant
        if (arguments != null) {
            Object injectedTenant = arguments.get("tenantId");
            if (injectedTenant == null) {
                injectedTenant = arguments.get("tenant_id");
            }
            if (injectedTenant != null) {
                String injectedStr = injectedTenant.toString().trim();
                if (!injectedStr.equalsIgnoreCase(authenticatedTenantId.toString())) {
                    log.error("ALERTA DE SEGURANÇA: Tentativa de violação cross-tenant detectada! Tenant autenticado: {}, Tenant injetado no argumento: {}",
                            authenticatedTenantId, injectedStr);
                    throw new SecurityException("Violação de segurança: Tentativa de acesso cross-tenant bloqueada na tool " + toolName);
                }
            }
        }

        Map<String, Object> safeArgs = arguments != null ? arguments : Map.of();
        log.debug("Executando tool '{}' para tenant={}", toolName, authenticatedTenantId);
        return tool.execute(authenticatedTenantId, safeArgs);
    }
}
