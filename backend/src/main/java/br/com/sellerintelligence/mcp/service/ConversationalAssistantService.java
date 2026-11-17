package br.com.sellerintelligence.mcp.service;

import br.com.sellerintelligence.discovery.security.UntrustedTextSanitizer;
import br.com.sellerintelligence.mcp.domain.*;
import br.com.sellerintelligence.mcp.web.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class ConversationalAssistantService {

    private static final Logger log = LoggerFactory.getLogger(ConversationalAssistantService.class);

    private final ConversationalSessionRepository sessionRepository;
    private final ConversationalMessageRepository messageRepository;
    private final McpToolRegistry toolRegistry;
    private final McpToolExecutorService toolExecutor;
    private final ConversationalAiProvider aiProvider;
    private final UntrustedTextSanitizer textSanitizer;

    private static final String SYSTEM_PROMPT = """
            Você é o assistente inteligente de vendas e finanças da Seller Intelligence Platform.
            Diretrizes Invioláveis:
            1. Você opera estritamente com ferramentas READ-ONLY do servidor MCP.
            2. Jamais execute SQL arbitrário, mutações, publicação de anúncios ou alteração de preços.
            3. Respostas devem sempre indicar período, escopo consultado e limitações.
            4. Dados indisponíveis devem ser categoricamente reportados como UNAVAILABLE. Jamais invente métricas financeiras.
            5. Cálculos de margem devem provir exclusivamente da ferramenta determinística calculate_sku_margin.
            """;

    public ConversationalAssistantService(ConversationalSessionRepository sessionRepository,
                                          ConversationalMessageRepository messageRepository,
                                          McpToolRegistry toolRegistry,
                                          McpToolExecutorService toolExecutor,
                                          ConversationalAiProvider aiProvider,
                                          UntrustedTextSanitizer textSanitizer) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
        this.aiProvider = aiProvider;
        this.textSanitizer = textSanitizer;
    }

    @Transactional
    public ConversationalChatResponse processChat(UUID tenantId, UUID userId, ConversationalChatRequest request, String ipAddress) {
        long startTime = System.currentTimeMillis();
        String rawMessage = request.message() != null ? request.message() : "";

        log.info("Processing conversational chat: tenantId={}, user={}, messageLength={}",
                tenantId, userId, rawMessage.length());

        // 1. Obtém ou cria a sessão de conversa
        ConversationalSession session;
        if (request.sessionId() != null) {
            session = sessionRepository.findByIdAndTenantId(request.sessionId(), tenantId)
                    .orElseGet(() -> sessionRepository.save(new ConversationalSession(tenantId, userId, "Conversa de Vendas")));
        } else {
            session = sessionRepository.save(new ConversationalSession(tenantId, userId, "Conversa de Vendas"));
        }

        // 2. Proteção Anti-Prompt Injection (OWASP LLM01)
        boolean hasSuspiciousInstructions = textSanitizer.containsSuspiciousInstructions(rawMessage);
        String sanitizedMessage = textSanitizer.sanitize(rawMessage);

        if (hasSuspiciousInstructions) {
            log.warn("Prompt injection attempt detected in chat message. Neutralizing payload: user={}", userId);
            String warningReply = "⚠️ Sua mensagem continha instruções adversariais ou tentativas de desvio de diretrizes de sistema que foram neutralizadas pelo filtro de segurança da plataforma. O assistente continuará respondendo estritamente dentro das políticas de integridade e isolamento de dados.";

            McpProvenanceDto securityProvenance = new McpProvenanceDto(
                    "MCP_SECURITY_GATE",
                    "UNAVAILABLE",
                    OffsetDateTime.now().toString(),
                    "USER_SESSION",
                    "Mensagem sanitizada contra prompt injection (OWASP LLM01)."
            );

            saveMessages(session.getId(), tenantId, sanitizedMessage, warningReply, "[]", "{}");

            return new ConversationalChatResponse(
                    session.getId(),
                    warningReply,
                    Collections.emptyList(),
                    securityProvenance,
                    List.of("Instruções suspeitas de injeção de prompt foram detectadas e desconsideradas."),
                    aiProvider.getProviderName(),
                    System.currentTimeMillis() - startTime
            );
        }

        // 3. Recupera histórico recente de mensagens da sessão
        List<ConversationalMessage> history = messageRepository
                .findBySessionIdAndTenantIdOrderByCreatedAtAsc(session.getId(), tenantId);

        // 4. Analisa intenção do usuário e decide ferramentas MCP
        ConversationalAiProvider.AiIntentDecision decision = aiProvider.analyzeAndDecide(
                SYSTEM_PROMPT,
                history,
                sanitizedMessage,
                toolRegistry.listTools(),
                request.modelOverride()
        );

        List<McpToolCallResponse> toolExecutions = new ArrayList<>();
        List<Object> toolDataList = new ArrayList<>();

        // 5. Executa ferramentas decididas
        if (decision.toolCalls() != null && !decision.toolCalls().isEmpty()) {
            for (McpToolCallRequest toolReq : decision.toolCalls()) {
                McpToolCallResponse toolResp = toolExecutor.executeTool(
                        tenantId, userId, null, toolReq, ipAddress
                );
                toolExecutions.add(toolResp);
                if (toolResp.success() && toolResp.data() != null) {
                    toolDataList.add(toolResp.data());
                }
            }
        }

        // 6. Sintetiza a resposta final embasada
        String finalReply;
        if (decision.directReply() != null && !decision.directReply().isBlank()) {
            finalReply = decision.directReply();
        } else {
            finalReply = aiProvider.synthesizeFinalReply(
                    SYSTEM_PROMPT,
                    sanitizedMessage,
                    toolDataList,
                    request.modelOverride()
            );
        }

        long totalLatency = System.currentTimeMillis() - startTime;

        McpProvenanceDto finalProvenance = new McpProvenanceDto(
                "Assistente Contextual + Servidor MCP Read-Only",
                toolDataList.isEmpty() ? "AI_GENERATED" : "OBSERVED",
                OffsetDateTime.now().toString(),
                "TENANT_ORGANIZATION",
                "Dados estritamente delimitados ao tenant autenticado; cálculos monetários derivados de motor determinístico."
        );

        List<String> limitations = List.of(
                "O assistente opera em modo estritamente read-only; operações mutáveis estão desabilitadas via MCP.",
                "Consultas a métricas financeiras refletem dados sincronizados até o momento da solicitação."
        );

        // 7. Persiste mensagens da conversa
        saveMessages(session.getId(), tenantId, sanitizedMessage, finalReply, "[]", "{}");

        return new ConversationalChatResponse(
                session.getId(),
                finalReply,
                toolExecutions,
                finalProvenance,
                limitations,
                aiProvider.getProviderName(),
                totalLatency
        );
    }

    private void saveMessages(UUID sessionId, UUID tenantId, String userContent, String assistantContent,
                              String toolCallsJson, String provenanceJson) {
        ConversationalMessage userMsg = new ConversationalMessage(
                sessionId, tenantId, ChatRole.USER, userContent, null, null
        );
        ConversationalMessage assistantMsg = new ConversationalMessage(
                sessionId, tenantId, ChatRole.ASSISTANT, assistantContent, toolCallsJson, provenanceJson
        );
        messageRepository.saveAll(List.of(userMsg, assistantMsg));
    }
}
