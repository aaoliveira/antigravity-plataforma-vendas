package br.com.sellerintelligence.mcp.web;

import br.com.sellerintelligence.tenancy.domain.TenantContext;
import br.com.sellerintelligence.mcp.domain.McpApiKey;
import br.com.sellerintelligence.mcp.domain.McpToolDefinition;
import br.com.sellerintelligence.mcp.domain.McpToolExecutionLog;
import br.com.sellerintelligence.mcp.domain.McpToolExecutionLogRepository;
import br.com.sellerintelligence.mcp.service.McpAuthService;
import br.com.sellerintelligence.mcp.service.McpToolExecutorService;
import br.com.sellerintelligence.mcp.service.McpToolRegistry;
import br.com.sellerintelligence.mcp.web.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/mcp")
public class McpServerController {

    private final McpToolRegistry toolRegistry;
    private final McpToolExecutorService toolExecutor;
    private final McpAuthService authService;
    private final McpToolExecutionLogRepository executionLogRepository;

    public McpServerController(McpToolRegistry toolRegistry,
                               McpToolExecutorService toolExecutor,
                               McpAuthService authService,
                               McpToolExecutionLogRepository executionLogRepository) {
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
        this.authService = authService;
        this.executionLogRepository = executionLogRepository;
    }

    // 1. Catálogo de Ferramentas Read-Only (MCP tools/list)
    @GetMapping("/tools")
    public ResponseEntity<List<McpToolDefinition>> listTools() {
        return ResponseEntity.ok(toolRegistry.listTools());
    }

    // 2. Execução de Ferramenta Autorizada (MCP tools/call)
    @PostMapping("/tools/call")
    public ResponseEntity<McpToolCallResponse> callTool(
            @RequestBody McpToolCallRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest httpRequest) {

        UUID tenantId;
        UUID userId = null;
        UUID apiKeyId = null;

        // Se houver chave MCP sip_mcp_..., autentica por ela
        if (authHeader != null && (authHeader.contains("sip_mcp_") || authHeader.startsWith("Bearer sip_mcp_"))) {
            Optional<McpApiKey> keyOpt = authService.authenticateApiKey(authHeader);
            if (keyOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            McpApiKey key = keyOpt.get();
            tenantId = key.getTenantId();
            apiKeyId = key.getId();
            userId = key.getCreatedBy();
        } else {
            // Caso contrário, resolve pelo TenantContext autenticado
            tenantId = TenantContext.getTenantId();
        }

        String ipAddress = httpRequest != null ? httpRequest.getRemoteAddr() : "127.0.0.1";
        McpToolCallResponse response = toolExecutor.executeTool(tenantId, userId, apiKeyId, request, ipAddress);

        return ResponseEntity.ok(response);
    }

    // 3. Gestão de Chaves de API MCP do Tenant
    @PostMapping("/api-keys")
    public ResponseEntity<ApiKeyCreatedResponse> createApiKey(
            @RequestBody CreateApiKeyRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId = UUID.randomUUID(); // Mock ou security principal
        ApiKeyCreatedResponse response = authService.createApiKey(tenantId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api-keys")
    public ResponseEntity<List<McpApiKey>> listApiKeys() {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(authService.listApiKeys(tenantId));
    }

    @DeleteMapping("/api-keys/{id}")
    public ResponseEntity<Void> revokeApiKey(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        boolean revoked = authService.revokeApiKey(tenantId, id);
        return revoked ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    // 4. Logs de Execução de Ferramentas MCP para Auditoria
    @GetMapping("/logs")
    public ResponseEntity<List<McpToolExecutionLog>> listExecutionLogs(
            @RequestParam(required = false) String toolName) {
        UUID tenantId = TenantContext.getTenantId();
        if (toolName != null && !toolName.isBlank()) {
            return ResponseEntity.ok(executionLogRepository.findByTenantIdAndToolNameOrderByCreatedAtDesc(tenantId, toolName));
        }
        return ResponseEntity.ok(executionLogRepository.findByTenantIdOrderByCreatedAtDesc(tenantId));
    }
}
