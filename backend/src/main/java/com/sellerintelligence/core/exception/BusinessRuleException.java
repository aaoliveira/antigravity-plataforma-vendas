package com.sellerintelligence.core.exception;

/**
 * Exceção disparada quando uma regra invariante de negócio é violada.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
