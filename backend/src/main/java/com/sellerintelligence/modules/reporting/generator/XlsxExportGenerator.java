package com.sellerintelligence.modules.reporting.generator;

import com.sellerintelligence.modules.reporting.model.ExportDataPage;
import com.sellerintelligence.modules.reporting.security.CsvFormulaSanitizer;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Gerador de planilhas XLSX usando Apache POI com cabeçalhos estilizados,
 * formatação de tipos nativos e sanitização preventiva de células string.
 */
@Component
public class XlsxExportGenerator {

    public byte[] generate(String sheetName, ExportDataPage dataPage) {
        String safeSheetName = (sheetName != null && !sheetName.isBlank()) ? sheetName : "Export";
        if (safeSheetName.length() > 31) {
            safeSheetName = safeSheetName.substring(0, 31);
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet(safeSheetName);

            // Estilos
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dateStyle = workbook.createCellStyle();
            CreationHelper creationHelper = workbook.getCreationHelper();
            dateStyle.setDataFormat(creationHelper.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));

            // Linha de Cabeçalho
            Row headerRow = sheet.createRow(0);
            List<String> headers = dataPage.headers();
            for (int col = 0; col < headers.size(); col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(headers.get(col));
                cell.setCellStyle(headerStyle);
            }

            // Linhas de Dados
            List<List<Object>> rows = dataPage.rows();
            for (int r = 0; r < rows.size(); r++) {
                Row row = sheet.createRow(r + 1);
                List<Object> rowValues = rows.get(r);
                for (int c = 0; c < rowValues.size(); c++) {
                    Cell cell = row.createCell(c);
                    Object val = rowValues.get(c);
                    setCellValue(cell, val, dateStyle);
                }
            }

            // Auto-ajuste de largura de colunas (máximo 50 caracteres)
            for (int col = 0; col < headers.size(); col++) {
                sheet.autoSizeColumn(col);
                int currentWidth = sheet.getColumnWidth(col);
                if (currentWidth > 15000) {
                    sheet.setColumnWidth(col, 15000);
                } else if (currentWidth < 3000) {
                    sheet.setColumnWidth(col, 3000);
                }
            }

            workbook.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao gerar arquivo XLSX: " + e.getMessage(), e);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        XSSFFont font = (XSSFFont) workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);

        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private void setCellValue(Cell cell, Object val, CellStyle dateStyle) {
        if (val == null) {
            cell.setBlank();
            return;
        }

        if (val instanceof BigDecimal bd) {
            cell.setCellValue(bd.doubleValue());
        } else if (val instanceof Number num) {
            cell.setCellValue(num.doubleValue());
        } else if (val instanceof Boolean b) {
            cell.setCellValue(b);
        } else if (val instanceof Instant instant) {
            cell.setCellValue(instant.toString());
        } else if (val instanceof LocalDate || val instanceof LocalDateTime) {
            cell.setCellValue(val.toString());
        } else {
            // String ou outro objeto: sanitiza contra injeção de fórmula
            String sanitized = CsvFormulaSanitizer.sanitize(val.toString());
            cell.setCellValue(sanitized);
        }
    }
}
