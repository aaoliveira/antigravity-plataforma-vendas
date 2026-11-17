package br.com.sellerintelligence.mcp.web.dto;

import java.util.Map;

public record McpToolCallRequest(
        String toolName,
        Map<String, Object> arguments
) {}
