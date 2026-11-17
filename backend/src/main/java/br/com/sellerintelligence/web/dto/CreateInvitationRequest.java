package br.com.sellerintelligence.web.dto;

import br.com.sellerintelligence.tenancy.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateInvitationRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotNull(message = "Role is required")
        Role role,

        List<UUID> allowedCompanyIds,

        List<UUID> allowedStoreIds
) {
}
