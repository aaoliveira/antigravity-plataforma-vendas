package br.com.sellerintelligence.web.dto;

import br.com.sellerintelligence.tenancy.domain.Role;
import br.com.sellerintelligence.tenancy.invitation.InvitationStatus;
import br.com.sellerintelligence.tenancy.invitation.UserInvitation;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InvitationResponse(
        UUID id,
        UUID tenantId,
        String email,
        Role role,
        String token,
        String allowedCompanyIds,
        String allowedStoreIds,
        InvitationStatus status,
        OffsetDateTime expiresAt,
        OffsetDateTime acceptedAt,
        OffsetDateTime createdAt
) {
    public static InvitationResponse from(UserInvitation inv) {
        return new InvitationResponse(
                inv.getId(),
                inv.getTenantId(),
                inv.getEmail(),
                inv.getRole(),
                inv.getToken(),
                inv.getAllowedCompanyIds(),
                inv.getAllowedStoreIds(),
                inv.getStatus(),
                inv.getExpiresAt(),
                inv.getAcceptedAt(),
                inv.getCreatedAt()
        );
    }
}
