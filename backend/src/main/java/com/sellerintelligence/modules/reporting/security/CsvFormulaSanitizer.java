package com.sellerintelligence.modules.reporting.security;

import java.util.regex.Pattern;

/**
 * Sanitizador de células contra CSV Formula Injection (CWE-1236 / OWASP ASVS).
 * Neutraliza células que iniciam com '=', '+', '-', '@', '\t', '\r', '|' ou variações com espaços iniciais.
 */
public final class CsvFormulaSanitizer {

    private static final Pattern SAFE_NUMERIC_PATTERN = Pattern.compile("^[-+]?\\d+([.,]\\d+)?$");

    private CsvFormulaSanitizer() {
        // Utility class
    }

    /**
     * Sanitiza o valor de uma célula para exportação segura em CSV ou XLSX.
     *
     * @param input valor original da célula
     * @return valor sanitizado com prefixo apóstrofo (') se contiver risco de injeção de fórmula
     */
    public static String sanitize(Object input) {
        if (input == null) {
            return "";
        }

        String value = input.toString();
        if (value.isEmpty()) {
            return "";
        }

        // Se for estritamente numérico simples (ex: "-10.50", "+500", "123"), não há risco de DDE/fórmula
        if (SAFE_NUMERIC_PATTERN.matcher(value.trim()).matches()) {
            return value;
        }

        // Verifica caracteres perigosos no início ou após espaços em branco
        String trimmed = value.stripLeading();
        if (trimmed.isEmpty()) {
            return value;
        }

        char firstChar = trimmed.charAt(0);
        if (isDangerousStartChar(firstChar)) {
            // Neutraliza prefixando com apóstrofo (')
            return "'" + value;
        }

        // Verifica caracteres de controle de evasão (\t, \r)
        if (value.startsWith("\t") || value.startsWith("\r") || value.startsWith("\n")) {
            return "'" + value;
        }

        return value;
    }

    private static boolean isDangerousStartChar(char ch) {
        return ch == '=' || ch == '+' || ch == '-' || ch == '@' || ch == '|' || ch == '\t' || ch == '\r';
    }
}
