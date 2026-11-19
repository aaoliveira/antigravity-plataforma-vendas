package com.sellerintelligence.modules.reporting.model;

/**
 * Formatos de arquivo suportados para exportação.
 */
public enum ExportFileFormat {
    CSV("text/csv; charset=UTF-8", ".csv"),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx");

    private final String contentType;
    private final String fileExtension;

    ExportFileFormat(String contentType, String fileExtension) {
        this.contentType = contentType;
        this.fileExtension = fileExtension;
    }

    public String getContentType() {
        return contentType;
    }

    public String getFileExtension() {
        return fileExtension;
    }
}
