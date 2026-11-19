package com.sellerintelligence.modules.aiassistant.security;

/**
 * Exceção de segurança disparada quando uma tentativa de Prompt Injection ou evasão de políticas é detectada.
 */
public class PromptInjectionException extends RuntimeException {

    private final String threatType;
    private final String offendingSnippet;

    public PromptInjectionException(String message, String threatType, String offendingSnippet) {
        super(message);
        this.threatType = threatType;
        this.offendingSnippet = offendingSnippet;
    }

    public String getThreatType() {
        return threatType;
    }

    public String getOffendingSnippet() {
        return offendingSnippet;
    }
}
