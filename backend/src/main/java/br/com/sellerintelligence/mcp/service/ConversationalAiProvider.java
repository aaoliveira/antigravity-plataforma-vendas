package br.com.sellerintelligence.mcp.service;

import br.com.sellerintelligence.mcp.domain.ConversationalMessage;
import br.com.sellerintelligence.mcp.domain.McpToolDefinition;
import br.com.sellerintelligence.mcp.web.dto.McpToolCallRequest;

import java.util.List;

public interface ConversationalAiProvider {

    record AiIntentDecision(
            String reasoning,
            List<McpToolCallRequest> toolCalls,
            String directReply
    ) {}

    AiIntentDecision analyzeAndDecide(
            String systemPrompt,
            List<ConversationalMessage> conversationHistory,
            String currentMessage,
            List<McpToolDefinition> availableTools,
            String modelOverride
    );

    String synthesizeFinalReply(
            String systemPrompt,
            String userMessage,
            List<Object> toolResults,
            String modelOverride
    );

    String getProviderName();
}
