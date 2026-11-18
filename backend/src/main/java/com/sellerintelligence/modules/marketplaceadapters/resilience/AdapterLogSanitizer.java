package com.sellerintelligence.modules.marketplaceadapters.resilience;

import java.util.regex.Pattern;

/**
 * Sanitizador de logs específico do conector de marketplace.
 * Assegura que nenhum token de acesso (OAuth/HMAC), chave secreta ou credencial seja logado.
 */
public final class AdapterLogSanitizer {

    private static final String REDACTED = "[REDACTED]";

    private static final Pattern SENSITIVE_KV = Pattern.compile(
            "\\b(access_token|refresh_token|secret|client_secret|partner_key|password|api_key)=[^&\\s,;]+",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SENSITIVE_JSON = Pattern.compile(
            "\"(access_token|refresh_token|secret|client_secret|partner_key|password|api_key)\"\\s*:\\s*\"[^\"]*\"",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern BEARER_PATTERN = Pattern.compile(
            "Bearer\\s+[A-Za-z0-9\\-_\\.]+",
            Pattern.CASE_INSENSITIVE
    );

    private AdapterLogSanitizer() {
    }

    public static String mask(String message) {
        if (message == null || message.isBlank()) {
            return message;
        }

        String masked = SENSITIVE_JSON.matcher(message)
                .replaceAll("\"$1\": \"" + REDACTED + "\"");

        masked = SENSITIVE_KV.matcher(masked)
                .replaceAll("$1=" + REDACTED);

        masked = BEARER_PATTERN.matcher(masked)
                .replaceAll("Bearer " + REDACTED);

        return masked;
    }
}
