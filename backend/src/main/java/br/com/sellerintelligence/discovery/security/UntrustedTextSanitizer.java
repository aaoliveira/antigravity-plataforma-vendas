package br.com.sellerintelligence.discovery.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Sanitizador de texto não confiável extraído de catálogos e arquivos de fornecedores.
 * Protege contra Prompt Injection indireto e exploração de contexto em conformidade com OWASP LLM01.
 */
@Component
public class UntrustedTextSanitizer {

    private static final Logger log = LoggerFactory.getLogger(UntrustedTextSanitizer.class);

    private static final Pattern INJECTION_PATTERNS = Pattern.compile(
            "(?i)(ignore\\s+previous\\s+instructions|system\\s+prompt|you\\s+are\\s+now|bypass\\s+guardrail|disregard\\s+all|override\\s+instructions|<script[\\s>]|<iframe[\\s>]|javascript:)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern HTML_TAGS_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern CONTROL_CHARS_PATTERN = Pattern.compile("[\\p{Cntrl}&&[^\r\n\t]]");

    /**
     * Sanitiza o texto extraído de documentos externos de fornecedores.
     */
    public String sanitize(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return "";
        }

        // 1. Remove caracteres de controle invisíveis
        String cleaned = CONTROL_CHARS_PATTERN.matcher(rawText).replaceAll("");

        // 2. Remove tags HTML/XML
        cleaned = HTML_TAGS_PATTERN.matcher(cleaned).replaceAll("");

        // 3. Detecta e neutraliza instruções adversariais de prompt injection
        if (INJECTION_PATTERNS.matcher(cleaned).find()) {
            log.warn("Potential prompt injection pattern detected in untrusted document text. Neutralizing payload.");
            cleaned = INJECTION_PATTERNS.matcher(cleaned).replaceAll("[REDACTED_SUSPICIOUS_PROMPT_INJECTION]");
        }

        return cleaned.trim();
    }

    /**
     * Retorna se o texto continha comandos suspeitos de injeção.
     */
    public boolean containsSuspiciousInstructions(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return false;
        }
        return INJECTION_PATTERNS.matcher(rawText).find();
    }
}
