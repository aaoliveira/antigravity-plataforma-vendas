package com.sellerintelligence.core.exception;

/**
 * Exceção lançada quando uma entidade já existe no tenant (conflito de chave única ou duplicidade).
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
