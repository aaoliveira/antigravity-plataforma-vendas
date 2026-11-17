package br.com.sellerintelligence.web.dto;

import br.com.sellerintelligence.tenancy.domain.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(
        @NotNull(message = "Role is required")
        Role role
) {
}
