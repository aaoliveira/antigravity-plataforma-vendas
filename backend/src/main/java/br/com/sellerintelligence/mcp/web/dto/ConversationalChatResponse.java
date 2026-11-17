package br.com.sellerintelligence.mcp.web.dto;

import java.util.List;
import java.util.UUID;

public record ConversationalChatResponse(
        UUID sessionId,
        String reply,
        List<McpToolCallResponse> toolExecutions,
        McpProvenanceDto provenance,
        List<String> limitations,
        String model,
        long latencyMs
) {}
