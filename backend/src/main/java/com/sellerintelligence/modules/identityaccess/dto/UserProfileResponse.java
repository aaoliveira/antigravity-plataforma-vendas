package com.sellerintelligence.modules.identityaccess.dto;

import java.util.List;
import java.util.UUID;

public record UserProfileResponse(
        UUID userId,
        String email,
        String fullName,
        String status,
        List<UserMembershipDto> memberships
) {
}
