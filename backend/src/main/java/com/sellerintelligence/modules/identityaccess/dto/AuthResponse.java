package com.sellerintelligence.modules.identityaccess.dto;

import java.util.List;
import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UUID activeTenantId,
        List<String> roles
) {
}
