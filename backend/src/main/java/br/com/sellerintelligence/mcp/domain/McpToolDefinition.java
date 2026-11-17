package br.com.sellerintelligence.mcp.domain;

import java.util.Map;

public record McpToolDefinition(
        String name,
        String description,
        boolean readOnly,
        Map<String, Object> inputSchema
) {
    public McpToolDefinition {
        if (!readOnly) {
            throw new IllegalArgumentException("MCP tools in this server must be strictly readOnly.");
        }
    }
}
