package br.com.sellerintelligence.tenancy.domain;

import br.com.sellerintelligence.infrastructure.error.BusinessException;
import org.springframework.http.HttpStatus;

public class TenantAccessDeniedException extends BusinessException {

    public TenantAccessDeniedException(String message) {
        super("TENANT_ACCESS_DENIED", message, HttpStatus.FORBIDDEN);
    }
}
