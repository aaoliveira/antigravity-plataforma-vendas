package com.sellerintelligence.core.security;

import org.springframework.security.core.AuthenticationException;

/**
 * Lançada quando o token JWT é malformado, possui assinatura inválida ou estrutura corrompida.
 */
public class InvalidTokenException extends AuthenticationException {

    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
