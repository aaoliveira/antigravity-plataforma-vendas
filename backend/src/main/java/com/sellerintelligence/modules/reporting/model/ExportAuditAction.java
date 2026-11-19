package com.sellerintelligence.modules.reporting.model;

/**
 * Ações de ciclo de vida e segurança registradas na auditoria de exportação.
 */
public enum ExportAuditAction {
    EXPORT_REQUESTED,
    EXPORT_COMPLETED,
    EXPORT_FAILED,
    EXPORT_DOWNLOADED,
    EXPORT_EXPIRED_ACCESS_ATTEMPT,
    EXPORT_CROSS_TENANT_ACCESS_BLOCKED
}
