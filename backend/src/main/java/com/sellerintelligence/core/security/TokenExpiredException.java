package com.sellerintelligence.core.security;

import org.springframework.security.core.AuthenticationException;

/**
 * Lançada quando o token JWT ultrapassou seu tempo limite de expiração (exp).
 */
public class TokenExpiredException extends AuthenticationException {

    public TokenExpiredException(String message) {
        super(message);
    }
}
