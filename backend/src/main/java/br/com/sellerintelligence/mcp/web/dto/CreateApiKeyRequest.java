package br.com.sellerintelligence.mcp.web.dto;

import java.util.UUID;

public record CreateApiKeyRequest(
        String name,
        Long expiresInDays
) {}
