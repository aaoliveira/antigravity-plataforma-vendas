package com.sellerintelligence.modules.marketplaceconnection.exception;

/**
 * Exceção lançada quando escopos não autorizados (ex: 'write' no MVP) são solicitados.
 */
public class UnauthorizedScopeException extends RuntimeException {

    public UnauthorizedScopeException(String message) {
        super(message);
    }
}
