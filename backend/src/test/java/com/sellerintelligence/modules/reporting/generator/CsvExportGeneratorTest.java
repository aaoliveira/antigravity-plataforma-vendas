package com.sellerintelligence.modules.reporting.generator;

import com.sellerintelligence.modules.reporting.model.ExportDataPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("P021-BE: Testes do CsvExportGenerator (RFC 4180 + Anti-Formula Injection)")
class CsvExportGeneratorTest {

    private final CsvExportGenerator generator = new CsvExportGenerator();

    @Test
    void shouldGenerateCsvWithBomAndHeaders() {
        List<String> headers = List.of("ID", "Produto", "Preco");
        List<List<Object>> rows = List.of(
                List.of("1", "Panela Inox", "150.00"),
                List.of("2", "Frigideira Antiaderente", "89.90")
        );
        ExportDataPage page = new ExportDataPage(headers, rows, 2, false);

        byte[] csvBytes = generator.generate(page);

        // Verifica BOM UTF-8 nos 3 primeiros bytes (0xEF, 0xBB, 0xBF)
        assertThat(csvBytes[0]).isEqualTo((byte) 0xEF);
        assertThat(csvBytes[1]).isEqualTo((byte) 0xBB);
        assertThat(csvBytes[2]).isEqualTo((byte) 0xBF);

        String csvContent = new String(csvBytes, StandardCharsets.UTF_8);
        assertThat(csvContent).contains("ID,Produto,Preco");
        assertThat(csvContent).contains("1,Panela Inox,150.00");
        assertThat(csvContent).contains("2,Frigideira Antiaderente,89.90");
    }

    @Test
    void shouldEscapeQuotesAndCommasProperly() {
        List<String> headers = List.of("SKU", "Descricao");
        List<List<Object>> rows = List.of(
                List.of("SKU-1", "Panela com \"revestimento especial\", 20cm"),
                List.of("SKU-2", "Produto com\nquebra de linha")
        );
        ExportDataPage page = new ExportDataPage(headers, rows, 2, false);

        byte[] csvBytes = generator.generate(page);
        String csvContent = new String(csvBytes, StandardCharsets.UTF_8);

        // Deve duplicar aspas internas e envolver entre aspas duplas
        assertThat(csvContent).contains("\"Panela com \"\"revestimento especial\"\", 20cm\"");
        assertThat(csvContent).contains("\"Produto com\nquebra de linha\"");
    }

    @Test
    void shouldSanitizeFormulasInCsv() {
        List<String> headers = List.of("ID", "Payload");
        List<List<Object>> rows = List.of(
                List.of("1", "=SUM(A1:B10)"),
                List.of("2", "-2+3*cmd|' /C calc'!A0"),
                List.of("3", "@HYPERLINK(\"http://evil.com\")")
        );
        ExportDataPage page = new ExportDataPage(headers, rows, 3, false);

        byte[] csvBytes = generator.generate(page);
        String csvContent = new String(csvBytes, StandardCharsets.UTF_8);

        // Fórmulas devem ser prefixadas com ' e escapadas
        assertThat(csvContent).contains("\"'=SUM(A1:B10)\"");
        assertThat(csvContent).contains("\"'-2+3*cmd|' /C calc'!A0\"");
        assertThat(csvContent).contains("\"'@HYPERLINK(\"\"http://evil.com\"\")\"");
    }
}
