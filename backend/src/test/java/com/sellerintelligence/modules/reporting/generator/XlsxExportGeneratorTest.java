package com.sellerintelligence.modules.reporting.generator;

import com.sellerintelligence.modules.reporting.model.ExportDataPage;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("P021-BE: Testes do XlsxExportGenerator (Apache POI + Tipagem e Sanitização)")
class XlsxExportGeneratorTest {

    private final XlsxExportGenerator generator = new XlsxExportGenerator();

    @Test
    void shouldGenerateValidXlsxWorkbook() throws IOException {
        List<String> headers = List.of("SKU", "Preco", "Ativo");
        List<List<Object>> rows = List.of(
                List.of("SKU-001", new BigDecimal("199.90"), true),
                List.of("SKU-002", 50, false)
        );
        ExportDataPage page = new ExportDataPage(headers, rows, 2, false);

        byte[] xlsxBytes = generator.generate("RelatorioVendas", page);
        assertThat(xlsxBytes).isNotEmpty();

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsxBytes))) {
            Sheet sheet = workbook.getSheet("RelatorioVendas");
            assertThat(sheet).isNotNull();

            // Cabeçalho
            Row headerRow = sheet.getRow(0);
            assertThat(headerRow.getCell(0).getStringCellValue()).isEqualTo("SKU");
            assertThat(headerRow.getCell(1).getStringCellValue()).isEqualTo("Preco");
            assertThat(headerRow.getCell(2).getStringCellValue()).isEqualTo("Ativo");

            // Linha 1: tipagem nativa
            Row row1 = sheet.getRow(1);
            assertThat(row1.getCell(0).getStringCellValue()).isEqualTo("SKU-001");
            assertThat(row1.getCell(1).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(row1.getCell(1).getNumericCellValue()).isEqualTo(199.90);
            assertThat(row1.getCell(2).getCellType()).isEqualTo(CellType.BOOLEAN);
            assertThat(row1.getCell(2).getBooleanCellValue()).isTrue();
        }
    }

    @Test
    void shouldSanitizeStringCellsInXlsx() throws IOException {
        List<String> headers = List.of("ID", "FormulaMaliciosa");
        List<List<Object>> rows = List.of(
                List.of("1", "=SUM(A1:B10)"),
                List.of("2", "-2+3*cmd|' /C calc'!A0")
        );
        ExportDataPage page = new ExportDataPage(headers, rows, 2, false);

        byte[] xlsxBytes = generator.generate("Seguranca", page);

        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsxBytes))) {
            Sheet sheet = workbook.getSheet("Seguranca");
            Row row1 = sheet.getRow(1);
            Row row2 = sheet.getRow(2);

            // Células com fórmulas devem ser convertidas em texto prefixadas com '
            assertThat(row1.getCell(1).getStringCellValue()).isEqualTo("'=SUM(A1:B10)");
            assertThat(row2.getCell(1).getStringCellValue()).isEqualTo("'-2+3*cmd|' /C calc'!A0");
        }
    }
}
