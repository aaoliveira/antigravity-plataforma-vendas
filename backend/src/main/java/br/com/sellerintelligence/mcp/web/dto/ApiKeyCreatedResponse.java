package br.com.sellerintelligence.mcp.web.dto;

import java.util.UUID;

public record ApiKeyCreatedResponse(
        UUID id,
        String name,
        String plainTextApiKey,
        String keyPrefix,
        String permissions,
        String expiresAt
) {}
