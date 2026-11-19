package com.sellerintelligence.modules.reporting.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.Collections;
import java.util.Map;

/**
 * Payload de solicitação de job de exportação.
 */
public record ExportRequestDto(
        @NotNull(message = "Dataset é obrigatório")
        ExportDataset dataset,

        ExportFileFormat fileFormat,

        String idempotencyKey,

        @Min(value = 1, message = "O limite mínimo de linhas é 1")
        @Max(value = 50000, message = "O limite máximo de linhas permitido é 50.000")
        Integer maxRows,

        Map<String, Object> filterParams
) {
    public ExportFileFormat resolvedFormat() {
        return fileFormat != null ? fileFormat : ExportFileFormat.CSV;
    }

    public int resolvedMaxRows() {
        return maxRows != null ? Math.min(Math.max(maxRows, 1), 50000) : 10000;
    }

    public Map<String, Object> resolvedFilterParams() {
        return filterParams != null ? filterParams : Collections.emptyMap();
    }
}
