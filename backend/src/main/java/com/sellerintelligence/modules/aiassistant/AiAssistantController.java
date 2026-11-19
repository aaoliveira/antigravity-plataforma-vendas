package com.sellerintelligence.modules.aiassistant;

import com.sellerintelligence.core.security.AuthenticatedUser;
import com.sellerintelligence.core.tenant.TenantContextHolder;
import com.sellerintelligence.modules.aiassistant.entity.AiUsageAuditRecord;
import com.sellerintelligence.modules.aiassistant.model.AiPromptRequestDto;
import com.sellerintelligence.modules.aiassistant.model.AiToolDefinitionDto;
import com.sellerintelligence.modules.aiassistant.model.StructuredAiResponseDto;
import com.sellerintelligence.modules.aiassistant.prompts.PromptTemplate;
import com.sellerintelligence.modules.aiassistant.service.AiGatewayService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Endpoint REST para o Gateway Multi-Provider de IA e Governança Analítica.
 * Operações são estritamente isoladas por tenant e auditadas.
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AiAssistantController {

    private final AiGatewayService aiGatewayService;

    public AiAssistantController(AiGatewayService aiGatewayService) {
        this.aiGatewayService = aiGatewayService;
    }

    /**
     * Executa solicitação assistiva de IA sob guardrails de governança.
     */
    @PostMapping("/execute")
    public ResponseEntity<StructuredAiResponseDto> executePrompt(
            @Valid @RequestBody AiPromptRequestDto request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        UUID userId = user != null ? user.getUserId() : null;
        StructuredAiResponseDto response = aiGatewayService.executePrompt(request, tenantId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retorna o catálogo de templates de prompt versionados e homologados.
     */
    @GetMapping("/prompts")
    public ResponseEntity<List<PromptTemplate>> listPrompts() {
        return ResponseEntity.ok(aiGatewayService.listPrompts());
    }

    /**
     * Retorna a lista de tools internas somente leitura registradas e seus schemas.
     */
    @GetMapping("/tools")
    public ResponseEntity<List<AiToolDefinitionDto>> listTools() {
        return ResponseEntity.ok(aiGatewayService.listTools());
    }

    /**
     * Consulta o histórico paginado de auditoria de consumo de IA e tokens do tenant.
     */
    @GetMapping("/audits")
    public ResponseEntity<Page<AiUsageAuditRecord>> getAudits(
            @RequestParam(required = false) String promptKey,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        Page<AiUsageAuditRecord> audits = aiGatewayService.searchAudits(
                tenantId, promptKey, provider, status, startDate, endDate, pageable
        );
        return ResponseEntity.ok(audits);
    }
}
