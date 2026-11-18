package com.sellerintelligence.core.security;

import org.springframework.security.core.AuthenticationException;

/**
 * Lançada quando um IP ou conta sofre bloqueio temporário após exceder o limite de tentativas com falha.
 */
public class BruteForceBlockedException extends AuthenticationException {

    public BruteForceBlockedException(String message) {
        super(message);
    }
}
