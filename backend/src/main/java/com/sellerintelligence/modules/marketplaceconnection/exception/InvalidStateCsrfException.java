package com.sellerintelligence.modules.marketplaceconnection.exception;

/**
 * Exceção lançada quando a validação do state anti-CSRF do OAuth falha,
 * prevenindo ataques de Cross-Site Request Forgery e Session Fixation.
 */
public class InvalidStateCsrfException extends RuntimeException {

    public InvalidStateCsrfException(String message) {
        super(message);
    }
}
