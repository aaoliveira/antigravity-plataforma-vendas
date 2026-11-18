package com.sellerintelligence.modules.tenancy;

import com.sellerintelligence.core.tenant.TenantMembershipValidator;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Implementação da validação de membership de tenant, consumida pelo filtro de segurança e contexto multi-tenant.
 */
@Component
public class TenancyMembershipValidatorImpl implements TenantMembershipValidator {

    private final MembershipRepository membershipRepository;

    public TenancyMembershipValidatorImpl(MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    @Override
    public boolean hasActiveMembership(UUID userId, UUID tenantId) {
        if (userId == null || tenantId == null) {
            return false;
        }
        return membershipRepository.existsByUserIdAndTenantIdAndStatus(userId, tenantId, "ACTIVE");
    }
}
