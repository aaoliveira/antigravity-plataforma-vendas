package com.sellerintelligence.modules.reporting.generator;

import com.sellerintelligence.modules.reporting.model.ExportDataPage;
import com.sellerintelligence.modules.reporting.security.CsvFormulaSanitizer;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Gerador de CSV conforme RFC 4180 com sanitização mandatória contra formula injection
 * e suporte a UTF-8 com BOM para compatibilidade com Microsoft Excel e Google Sheets.
 */
@Component
public class CsvExportGenerator {

    private static final byte[] UTF8_BOM = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    public byte[] generate(ExportDataPage dataPage) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // Emite BOM UTF-8 para que o Excel abra acentuações em português nativamente
            baos.write(UTF8_BOM);

            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
                // Escreve cabeçalhos
                writeLine(writer, dataPage.headers());

                // Escreve linhas de dados sanitizadas
                for (List<Object> row : dataPage.rows()) {
                    writeLine(writer, row);
                }

                writer.flush();
            }

            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao gerar arquivo CSV: " + e.getMessage(), e);
        }
    }

    private void writeLine(PrintWriter writer, List<?> values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            Object val = values.get(i);
            sb.append(escapeAndSanitizeCell(val));
        }
        writer.println(sb.toString());
    }

    private String escapeAndSanitizeCell(Object cellValue) {
        if (cellValue == null) {
            return "";
        }

        // 1. Sanitização estrita contra CSV formula injection
        String sanitized = CsvFormulaSanitizer.sanitize(cellValue);

        // 2. Escape RFC 4180: se contiver vírgula, aspas, quebra de linha ou apóstrofo, envolve entre aspas
        boolean mustQuote = sanitized.contains(",") ||
                sanitized.contains("\"") ||
                sanitized.contains("\n") ||
                sanitized.contains("\r") ||
                sanitized.startsWith("'");

        if (mustQuote) {
            // Duplica aspas internas conforme RFC 4180
            String escaped = sanitized.replace("\"", "\"\"");
            return "\"" + escaped + "\"";
        }

        return sanitized;
    }
}
