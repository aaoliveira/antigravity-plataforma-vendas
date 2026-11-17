package br.com.sellerintelligence.sync.csv;

import java.util.List;

public record CsvValidationReport(
        String resourceType,
        int totalRows,
        int importedRows,
        int rejectedRows,
        List<CsvRowError> errors
) {
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }
}
