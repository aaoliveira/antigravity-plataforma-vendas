package br.com.sellerintelligence.web.dto;

import br.com.sellerintelligence.tenancy.domain.Organization;
import br.com.sellerintelligence.tenancy.domain.OrganizationStatus;
import br.com.sellerintelligence.tenancy.domain.Role;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String name,
        String slug,
        OrganizationStatus status,
        Role userRole,
        OffsetDateTime createdAt
) {
    public static OrganizationResponse from(Organization org, Role userRole) {
        return new OrganizationResponse(
                org.getId(),
                org.getName(),
                org.getSlug(),
                org.getStatus(),
                userRole,
                org.getCreatedAt()
        );
    }
}
