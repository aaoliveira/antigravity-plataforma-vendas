package com.sellerintelligence.modules.marketintelligence.service;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.regex.Pattern;

/**
 * Componente de sanitização e minimização de dados de observações de mercado.
 * Remove dados pessoais (LGPD), credenciais, tokens e parâmetros de rastreamento sensíveis.
 */
@Component
public class MarketObservationSanitizer {

    private static final Pattern SENSITIVE_QUERY_PARAMS = Pattern.compile(
            "(?i)(&|\\?)(access_token|refresh_token|token|secret|password|senha|auth|client_secret|api_key|session_id|jwt)=[^&]*"
    );

    private static final Pattern SENSITIVE_JSON_FIELDS = Pattern.compile(
            "(?i)\"(password|senha|token|access_token|refresh_token|secret|authorization|cpf|cnpj|cartao|credit_card|cvv)\"\\s*:\\s*\"[^\"]*\""
    );

    /**
     * Sanitiza a URL de origem: valida formato e remove parâmetros que possam carregar credenciais ou tokens.
     */
    public String sanitizeSourceUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return rawUrl;
        }

        try {
            URI uri = URI.create(rawUrl.trim());
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                throw new IllegalArgumentException("A URL de origem deve utilizar protocolo HTTP ou HTTPS.");
            }

            // Remove query params sensíveis
            String sanitized = SENSITIVE_QUERY_PARAMS.matcher(rawUrl.trim()).replaceAll("");
            // Normaliza caracteres finais após remoção
            if (sanitized.endsWith("?")) {
                sanitized = sanitized.substring(0, sanitized.length() - 1);
            }
            return sanitized;
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                throw e;
            }
            throw new IllegalArgumentException("URL de origem com formato inválido: " + e.getMessage());
        }
    }

    /**
     * Minimiza e sanitiza o payload JSON original, eliminando dados sensíveis e campos confidenciais.
     */
    public String minimizePayload(String rawPayload) {
        if (rawPayload == null || rawPayload.isBlank()) {
            return null;
        }

        // Remove tokens e credenciais
        String cleaned = SENSITIVE_JSON_FIELDS.matcher(rawPayload).replaceAll("\"$1\":\"[REDACTED]\"");

        // Limita o tamanho máximo para evitar estouro de armazenamento (máx 16KB)
        if (cleaned.length() > 16384) {
            cleaned = cleaned.substring(0, 16384) + "... [truncated]";
        }

        return cleaned;
    }
}
