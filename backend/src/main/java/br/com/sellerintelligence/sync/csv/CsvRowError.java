package br.com.sellerintelligence.sync.csv;

public record CsvRowError(
        int lineNumber,
        String field,
        String rejectedValue,
        String reason
) {}
