package com.sellerintelligence.core.tenant;

import java.util.UUID;

/**
 * Contrato de infraestrutura para validação de membership entre usuário e organização.
 * Permite que a camada core valide privilégios de tenant sem acoplar a módulos de domínio.
 */
public interface TenantMembershipValidator {

    /**
     * Valida se o usuário informado possui membership ativa na organização/tenant informada.
     */
    boolean hasActiveMembership(UUID userId, UUID tenantId);
}
