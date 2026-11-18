package com.sellerintelligence.modules.tenancy;

import com.sellerintelligence.core.exception.ResourceNotFoundException;
import com.sellerintelligence.core.security.AuthenticatedUser;
import com.sellerintelligence.core.tenant.TenantContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Controller tenant-scoped para consulta do contexto organizacional em vigor.
 */
@RestController
@RequestMapping("/api/v1/tenancy")
@Tag(name = "Tenancy", description = "Operações e verificação de contexto da organização/tenant ativo")
public class TenancyController {

    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;

    public TenancyController(
            OrganizationRepository organizationRepository,
            MembershipRepository membershipRepository
    ) {
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
    }

    @GetMapping("/current")
    @Operation(summary = "Retorna o contexto do tenant ativo validado", description = "Requer autenticação e contexto de tenant ativo resolvido")
    public ResponseEntity<CurrentTenantResponse> getCurrentTenant(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();

        Organization organization = organizationRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Organização não encontrada para o tenantId: " + tenantId));

        String role = "MEMBER";
        if (authenticatedUser != null) {
            role = membershipRepository.findByUserIdAndTenantIdAndStatus(authenticatedUser.getUserId(), tenantId, "ACTIVE")
                    .map(Membership::getRole)
                    .orElse("MEMBER");
        }

        return ResponseEntity.ok(new CurrentTenantResponse(
                organization.getId(),
                organization.getName(),
                organization.getSlug(),
                role,
                organization.getStatus()
        ));
    }

    public record CurrentTenantResponse(
            UUID tenantId,
            String name,
            String slug,
            String role,
            String status
    ) {
    }
}
