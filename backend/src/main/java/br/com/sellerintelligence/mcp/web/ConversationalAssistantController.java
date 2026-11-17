package br.com.sellerintelligence.mcp.web;

import br.com.sellerintelligence.tenancy.domain.TenantContext;
import br.com.sellerintelligence.mcp.domain.ConversationalMessage;
import br.com.sellerintelligence.mcp.domain.ConversationalMessageRepository;
import br.com.sellerintelligence.mcp.domain.ConversationalSession;
import br.com.sellerintelligence.mcp.domain.ConversationalSessionRepository;
import br.com.sellerintelligence.mcp.service.ConversationalAssistantService;
import br.com.sellerintelligence.mcp.web.dto.ConversationalChatRequest;
import br.com.sellerintelligence.mcp.web.dto.ConversationalChatResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai/conversational")
public class ConversationalAssistantController {

    private final ConversationalAssistantService assistantService;
    private final ConversationalSessionRepository sessionRepository;
    private final ConversationalMessageRepository messageRepository;

    public ConversationalAssistantController(ConversationalAssistantService assistantService,
                                             ConversationalSessionRepository sessionRepository,
                                             ConversationalMessageRepository messageRepository) {
        this.assistantService = assistantService;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
    }

    @PostMapping("/chat")
    public ResponseEntity<ConversationalChatResponse> chat(
            @RequestBody ConversationalChatRequest request,
            HttpServletRequest httpRequest) {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId = UUID.randomUUID(); // Mock/Keycloak sub
        String ipAddress = httpRequest != null ? httpRequest.getRemoteAddr() : "127.0.0.1";

        ConversationalChatResponse response = assistantService.processChat(tenantId, userId, request, ipAddress);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<ConversationalSession>> listSessions() {
        UUID tenantId = TenantContext.getTenantId();
        UUID userId = UUID.randomUUID();
        return ResponseEntity.ok(sessionRepository.findByTenantIdAndUserIdOrderByUpdatedAtDesc(tenantId, userId));
    }

    @GetMapping("/sessions/{id}/messages")
    public ResponseEntity<List<ConversationalMessage>> listMessages(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(messageRepository.findBySessionIdAndTenantIdOrderByCreatedAtAsc(id, tenantId));
    }
}
