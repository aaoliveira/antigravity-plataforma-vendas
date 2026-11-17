package br.com.sellerintelligence.mcp.web.dto;

import java.util.UUID;

public record ConversationalChatRequest(
        UUID sessionId,
        String message,
        String marketplace,
        String modelOverride
) {}
