package br.com.sellerintelligence.mcp.web.dto;

import br.com.sellerintelligence.mcp.domain.McpExecutionStatus;

public record McpToolCallResponse(
        boolean success,
        String toolName,
        Object data,
        String errorMessage,
        McpExecutionStatus status,
        McpProvenanceDto provenance,
        long latencyMs
) {}
