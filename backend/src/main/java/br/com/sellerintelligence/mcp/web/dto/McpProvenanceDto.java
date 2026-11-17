package br.com.sellerintelligence.mcp.web.dto;

public record McpProvenanceDto(
        String source,
        String classification,
        String collectedAt,
        String scope,
        String limitations
) {}
