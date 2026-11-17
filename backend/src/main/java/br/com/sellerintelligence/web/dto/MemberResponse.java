package br.com.sellerintelligence.web.dto;

import br.com.sellerintelligence.tenancy.domain.Membership;
import br.com.sellerintelligence.tenancy.domain.MembershipStatus;
import br.com.sellerintelligence.tenancy.domain.Role;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MemberResponse(
        UUID membershipId,
        UUID userId,
        String email,
        String name,
        Role role,
        MembershipStatus status,
        OffsetDateTime joinedAt
) {
    public static MemberResponse from(Membership membership) {
        return new MemberResponse(
                membership.getId(),
                membership.getUser().getId(),
                membership.getUser().getEmail(),
                membership.getUser().getName(),
                membership.getRole(),
                membership.getStatus(),
                membership.getCreatedAt()
        );
    }
}
