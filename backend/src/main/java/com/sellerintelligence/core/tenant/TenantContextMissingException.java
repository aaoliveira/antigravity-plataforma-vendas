package com.sellerintelligence.core.tenant;

/**
 * Lançada quando uma operação tenant-scoped é executada sem um contexto de tenant ativo.
 */
public class TenantContextMissingException extends RuntimeException {

    public TenantContextMissingException(String message) {
        super(message);
    }
}
