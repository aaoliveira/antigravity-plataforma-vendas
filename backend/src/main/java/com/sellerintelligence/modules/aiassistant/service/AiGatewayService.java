package com.sellerintelligence.modules.aiassistant.service;

import com.sellerintelligence.modules.aiassistant.entity.AiUsageAuditRecord;
import com.sellerintelligence.modules.aiassistant.gateway.AiModelConfig;
import com.sellerintelligence.modules.aiassistant.gateway.AiProvider;
import com.sellerintelligence.modules.aiassistant.gateway.AiProviderClient;
import com.sellerintelligence.modules.aiassistant.gateway.AiSecretManager;
import com.sellerintelligence.modules.aiassistant.model.AiPromptRequestDto;
import com.sellerintelligence.modules.aiassistant.model.AiToolDefinitionDto;
import com.sellerintelligence.modules.aiassistant.model.AiUsageMetricsDto;
import com.sellerintelligence.modules.aiassistant.model.StructuredAiResponseDto;
import com.sellerintelligence.modules.aiassistant.prompts.PromptCatalog;
import com.sellerintelligence.modules.aiassistant.prompts.PromptTemplate;
import com.sellerintelligence.modules.aiassistant.repository.AiUsageAuditRepository;
import com.sellerintelligence.modules.aiassistant.security.PiiSanitizer;
import com.sellerintelligence.modules.aiassistant.security.PromptInjectionDefenseService;
import com.sellerintelligence.modules.aiassistant.security.PromptInjectionException;
import com.sellerintelligence.modules.aiassistant.tools.AiToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Serviço orquestrador do Gateway de IA da Seller Intelligence Platform.
 * Integra Secrets em Secret Manager, Defesa contra Prompt Injection,
 * Versionamento de Prompts, Tool Calling Hermético e Auditoria Persistida.
 */
@Service
public class AiGatewayService {

    private static final Logger log = LoggerFactory.getLogger(AiGatewayService.class);

    private static final String ANTI_HALLUCINATION_DISCLAIMER =
            "Valores e projeções calculados deterministicamente pelo Seller Intelligence Platform. " +
            "Métricas não fornecidas pelas APIs oficiais dos marketplaces são explicitamente sinalizadas como indisponíveis, prevenindo alucinações.";

    private final AiSecretManager secretManager;
    private final AiProviderClient providerClient;
    private final AiToolRegistry toolRegistry;
    private final PromptCatalog promptCatalog;
    private final PromptInjectionDefenseService defenseService;
    private final PiiSanitizer piiSanitizer;
    private final AiUsageAuditRepository auditRepository;

    public AiGatewayService(
            AiSecretManager secretManager,
            AiProviderClient providerClient,
            AiToolRegistry toolRegistry,
            PromptCatalog promptCatalog,
            PromptInjectionDefenseService defenseService,
            PiiSanitizer piiSanitizer,
            AiUsageAuditRepository auditRepository
    ) {
        this.secretManager = secretManager;
        this.providerClient = providerClient;
        this.toolRegistry = toolRegistry;
        this.promptCatalog = promptCatalog;
        this.defenseService = defenseService;
        this.piiSanitizer = piiSanitizer;
        this.auditRepository = auditRepository;
    }

    /**
     * Executa solicitação assistiva de IA garantindo isolamento de tenant,
     * prevenção de injeção de comandos e auditoria mandatória de custos e tokens.
     */
    @Transactional
    public StructuredAiResponseDto executePrompt(AiPromptRequestDto request, UUID tenantId, UUID userId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID autenticado é obrigatório para execução no AI Gateway.");
        }

        // 1. Defesa em Profundidade contra Prompt Injection
        validateInputsAgainstInjection(request, tenantId, userId);

        // 2. Resolução do Prompt Template Versionado
        PromptTemplate template = promptCatalog.getTemplate(request.promptKey(), request.promptVersion())
                .orElseThrow(() -> new IllegalArgumentException("Prompt '" + request.promptKey() + "' não encontrado no catálogo oficial."));

        // 3. Resolução de Provedor e Configuração Operacional
        AiProvider provider = resolveProvider(request.provider());
        AiModelConfig config = AiModelConfig.forProvider(provider);
        String modelName = (request.model() != null && !request.model().isBlank())
                ? request.model()
                : config.defaultModelName();

        // 4. Sanitização de PII antes de encaminhar ao Provedor
        String sanitizedUserInput = piiSanitizer.sanitize(request.userInput());

        Instant start = Instant.now();
        AiProviderClient.ProviderExecutionResult result;
        try {
            result = providerClient.execute(
                    provider,
                    modelName,
                    template,
                    sanitizedUserInput,
                    request.entityContext(),
                    toolRegistry,
                    tenantId,
                    config
            );
        } catch (Exception e) {
            log.error("Falha na execução do provedor {} para tenant {}: {}", provider, tenantId, e.getMessage());
            persistFailureAudit(tenantId, userId, provider, modelName, template, e);
            throw e;
        }

        // 5. Cálculo Determinístico de Custos e Métricas
        BigDecimal estimatedCostBrl = config.calculateEstimatedCost(result.promptTokens(), result.completionTokens());
        int totalTokens = result.promptTokens() + result.completionTokens();
        String toolsInvoked = String.join(",", result.toolsExecuted());

        // 6. Gravação Mandatória de Auditoria de Consumo
        AiUsageAuditRecord auditRecord = new AiUsageAuditRecord(
                UUID.randomUUID(),
                tenantId,
                userId,
                provider.name(),
                modelName,
                template.key(),
                template.version(),
                result.promptTokens(),
                result.completionTokens(),
                totalTokens,
                result.latencyMs(),
                estimatedCostBrl,
                toolsInvoked,
                false,
                null,
                "SUCCESS",
                null,
                null,
                Instant.now()
        );
        auditRepository.save(auditRecord);

        // 7. Resposta Estruturada com Fontes e Guardrails
        AiUsageMetricsDto usageMetrics = new AiUsageMetricsDto(
                result.promptTokens(),
                result.completionTokens(),
                totalTokens,
                result.latencyMs(),
                estimatedCostBrl
        );

        return new StructuredAiResponseDto(
                auditRecord.getId(),
                tenantId,
                provider,
                modelName,
                template.key(),
                template.version(),
                result.textContent(),
                result.structuredData(),
                result.sources(),
                Map.of("executedAt", Instant.now(), "promptCreatedAt", template.registeredAt()),
                result.toolsExecuted(),
                usageMetrics,
                result.unavailabilityNotes(),
                ANTI_HALLUCINATION_DISCLAIMER
        );
    }

    private void validateInputsAgainstInjection(AiPromptRequestDto request, UUID tenantId, UUID userId) {
        if (request.userInput() != null) {
            PromptInjectionDefenseService.ScanResult scan = defenseService.scan(request.userInput(), "userInput");
            if (!scan.clean()) {
                persistSecurityThreatAudit(tenantId, userId, request, scan);
                throw new PromptInjectionException(
                        "Entrada bloqueada por violação das políticas de segurança e integridade (Prompt Injection detectado).",
                        scan.threatType(),
                        scan.offendingSnippet()
                );
            }
        }

        if (request.entityContext() != null) {
            for (Map.Entry<String, Object> entry : request.entityContext().entrySet()) {
                if (entry.getValue() instanceof String strVal) {
                    PromptInjectionDefenseService.ScanResult scan = defenseService.scan(strVal, "entityContext." + entry.getKey());
                    if (!scan.clean()) {
                        persistSecurityThreatAudit(tenantId, userId, request, scan);
                        throw new PromptInjectionException(
                                "Parâmetro contextual de entrada bloqueado por conter padrão malicioso de injeção.",
                                scan.threatType(),
                                scan.offendingSnippet()
                        );
                    }
                }
            }
        }
    }

    private AiProvider resolveProvider(AiProvider requested) {
        if (requested != null) {
            return requested;
        }
        if (secretManager.hasCredentials(AiProvider.GEMINI)) {
            return AiProvider.GEMINI;
        }
        return AiProvider.MOCK_PROVIDER;
    }

    private void persistSecurityThreatAudit(
            UUID tenantId,
            UUID userId,
            AiPromptRequestDto request,
            PromptInjectionDefenseService.ScanResult scan
    ) {
        try {
            AiUsageAuditRecord threatRecord = new AiUsageAuditRecord(
                    UUID.randomUUID(),
                    tenantId,
                    userId,
                    request.provider() != null ? request.provider().name() : AiProvider.MOCK_PROVIDER.name(),
                    request.model() != null ? request.model() : "security-filter",
                    request.promptKey() != null ? request.promptKey() : "UNKNOWN",
                    request.promptVersion() != null ? request.promptVersion() : "v1.0.0",
                    0,
                    0,
                    0,
                    0,
                    BigDecimal.ZERO,
                    null,
                    true,
                    scan.threatType(),
                    "BLOCKED_SECURITY",
                    "PROMPT_INJECTION_DETECTED",
                    "snippet=" + scan.offendingSnippet(),
                    Instant.now()
            );
            auditRepository.save(threatRecord);
        } catch (Exception ex) {
            log.error("Erro ao registrar auditoria de ameaça de segurança: {}", ex.getMessage());
        }
    }

    private void persistFailureAudit(
            UUID tenantId,
            UUID userId,
            AiProvider provider,
            String modelName,
            PromptTemplate template,
            Exception e
    ) {
        try {
            AiUsageAuditRecord failRecord = new AiUsageAuditRecord(
                    UUID.randomUUID(),
                    tenantId,
                    userId,
                    provider.name(),
                    modelName,
                    template != null ? template.key() : "UNKNOWN",
                    template != null ? template.version() : "v1.0.0",
                    0,
                    0,
                    0,
                    0,
                    BigDecimal.ZERO,
                    null,
                    false,
                    null,
                    "FAILED",
                    e.getClass().getSimpleName(),
                    e.getMessage(),
                    Instant.now()
            );
            auditRepository.save(failRecord);
        } catch (Exception ex) {
            log.error("Erro ao registrar auditoria de falha de execução de IA: {}", ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<PromptTemplate> listPrompts() {
        return promptCatalog.listAllTemplates();
    }

    @Transactional(readOnly = true)
    public List<AiToolDefinitionDto> listTools() {
        return toolRegistry.listAllTools().stream()
                .map(tool -> new AiToolDefinitionDto(
                        tool.getName(),
                        tool.getDescription(),
                        tool.getParametersSchema()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<AiUsageAuditRecord> searchAudits(
            UUID tenantId,
            String promptKey,
            String provider,
            String status,
            Instant startDate,
            Instant endDate,
            Pageable pageable
    ) {
        if (tenantId == null) {
            throw new IllegalArgumentException("Tenant ID é obrigatório para consultar auditoria.");
        }
        return auditRepository.searchAudits(tenantId, promptKey, provider, status, startDate, endDate, pageable);
    }
}
