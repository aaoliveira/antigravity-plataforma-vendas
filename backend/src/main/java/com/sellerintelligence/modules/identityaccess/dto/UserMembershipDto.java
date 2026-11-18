package com.sellerintelligence.modules.identityaccess.dto;

import java.util.UUID;

public record UserMembershipDto(
        UUID tenantId,
        String tenantName,
        String role,
        String status
) {
}
