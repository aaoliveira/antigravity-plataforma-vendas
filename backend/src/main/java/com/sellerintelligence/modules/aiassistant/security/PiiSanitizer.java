package com.sellerintelligence.modules.aiassistant.security;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Utilitário para sanitização estrita de Informações de Identificação Pessoal (PII)
 * e credenciais de segurança antes de logging ou gravação de auditoria.
 */
@Component
public class PiiSanitizer {

    // CPF: 000.000.000-00 ou 11 dígitos contínuos
    private static final Pattern CPF_PATTERN = Pattern.compile("\\b\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}\\b|\\b\\d{11}\\b");

    // CNPJ: 00.000.000/0000-00
    private static final Pattern CNPJ_PATTERN = Pattern.compile("\\b\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}\\b");

    // E-mail: x@y.z
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");

    // Bearer Token / API Key
    private static final Pattern BEARER_PATTERN = Pattern.compile("(?i)(bearer\\s+)[a-zA-Z0-9_.-]+");
    private static final Pattern API_KEY_PATTERN = Pattern.compile("(?i)(sk-[a-zA-Z0-9_-]{8,})");

    /**
     * Remove ou mascara PII e credenciais da mensagem informada.
     */
    public String sanitize(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        String result = input;
        result = BEARER_PATTERN.matcher(result).replaceAll("$1[REDACTED_TOKEN]");
        result = API_KEY_PATTERN.matcher(result).replaceAll("sk-***[REDACTED]");
        result = EMAIL_PATTERN.matcher(result).replaceAll("[REDACTED_EMAIL]");
        result = CPF_PATTERN.matcher(result).replaceAll("[REDACTED_CPF]");
        result = CNPJ_PATTERN.matcher(result).replaceAll("[REDACTED_CNPJ]");

        return result;
    }
}
