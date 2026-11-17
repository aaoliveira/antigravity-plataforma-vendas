package br.com.sellerintelligence.web.dto;

import br.com.sellerintelligence.tenancy.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddMemberRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        String name,

        @NotNull(message = "Role is required")
        Role role
) {
}
