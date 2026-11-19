package com.sellerintelligence.modules.reporting.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Representa os dados tabulares a serem exportados para um determinado dataset.
 */
public record ExportDataPage(
        List<String> headers,
        List<List<Object>> rows,
        int totalCount,
        boolean truncated
) {
    public ExportDataPage {
        headers = headers != null ? List.copyOf(headers) : Collections.emptyList();
        rows = rows != null ? List.copyOf(rows) : Collections.emptyList();
    }

    public static ExportDataPage empty(List<String> headers) {
        return new ExportDataPage(headers, Collections.emptyList(), 0, false);
    }
}
