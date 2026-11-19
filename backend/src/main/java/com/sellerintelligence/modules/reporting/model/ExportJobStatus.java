package com.sellerintelligence.modules.reporting.model;

/**
 * Estados do ciclo de vida de um job de exportação.
 */
public enum ExportJobStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    EXPIRED
}
