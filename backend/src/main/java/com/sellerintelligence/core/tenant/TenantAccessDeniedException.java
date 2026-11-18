package com.sellerintelligence.core.tenant;

/**
 * Lançada quando há tentativa de acesso a um tenant sem membership ou tentativa de spoofing.
 */
public class TenantAccessDeniedException extends RuntimeException {

    public TenantAccessDeniedException(String message) {
        super(message);
    }
}
