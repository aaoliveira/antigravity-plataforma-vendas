package com.sellerintelligence.core.tenant;

import java.util.UUID;

/**
 * Contexto imutável do tenant associado à requisição em execução.
 */
public record TenantContext(UUID tenantId, String role) {

    public TenantContext {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId não pode ser nulo no TenantContext");
        }
    }
}
