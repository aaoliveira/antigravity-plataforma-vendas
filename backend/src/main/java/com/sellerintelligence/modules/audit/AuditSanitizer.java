package com.sellerintelligence.modules.audit;

import java.util.regex.Pattern;

/**
 * Utilitário para sanitização estrita de dados antes de registrar em auditoria e logs.
 * Impede terminantemente a persistência de senhas, tokens de acesso, refresh tokens,
 * segredos de clientes ou chaves de API.
 */
public final class AuditSanitizer {

    private static final String REDACTED = "[REDACTED]";

    // Regex para campos JSON com senhas ou segredos
    private static final Pattern JSON_PASSWORD_PATTERN = Pattern.compile(
            "\"(password|senha|secret|client_secret|api_key|access_token|refresh_token|token)\"\\s*:\\s*\"[^\"]*\"",
            Pattern.CASE_INSENSITIVE
    );

    // Regex para query params ou key-value pairs (ex: password=secret123)
    private static final Pattern KV_PASSWORD_PATTERN = Pattern.compile(
            "\\b(password|senha|secret|client_secret|api_key|access_token|refresh_token|token)=[^&\\s,;]+",
            Pattern.CASE_INSENSITIVE
    );

    // Regex para Bearer tokens HTTP (ex: Bearer eyJhbGci...)
    private static final Pattern BEARER_TOKEN_PATTERN = Pattern.compile(
            "Bearer\\s+[A-Za-z0-9\\-_\\.]+",
            Pattern.CASE_INSENSITIVE
    );

    // Regex para tokens JWT soltos (iniciando com eyJ...)
    private static final Pattern RAW_JWT_PATTERN = Pattern.compile(
            "\\beyJ[A-Za-z0-9\\-_]{10,}\\.[A-Za-z0-9\\-_]{10,}\\.[A-Za-z0-9\\-_]+\\b"
    );

    private AuditSanitizer() {
    }

    /**
     * Sanitiza a string de entrada, substituindo credenciais ou tokens sensíveis por [REDACTED].
     *
     * @param input texto contendo detalhes, payloads ou metadados
     * @return texto devidamente sanitizado ou null se entrada for null
     */
    public static String sanitize(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        String result = input;

        // 1. Substituir valores em formato JSON
        result = JSON_PASSWORD_PATTERN.matcher(result)
                .replaceAll("\"$1\": \"" + REDACTED + "\"");

        // 2. Substituir pares chave=valor
        result = KV_PASSWORD_PATTERN.matcher(result)
                .replaceAll("$1=" + REDACTED);

        // 3. Substituir Bearer tokens
        result = BEARER_TOKEN_PATTERN.matcher(result)
                .replaceAll("Bearer " + REDACTED);

        // 4. Substituir JWTs soltos
        result = RAW_JWT_PATTERN.matcher(result)
                .replaceAll(REDACTED);

        return result;
    }
}
