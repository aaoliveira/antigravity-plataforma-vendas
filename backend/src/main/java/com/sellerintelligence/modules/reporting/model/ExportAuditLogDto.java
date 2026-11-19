package com.sellerintelligence.modules.reporting.model;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO para registros de auditoria de exportação.
 */
public record ExportAuditLogDto(
        UUID id,
        UUID tenantId,
        UUID exportJobId,
        ExportAuditAction action,
        String userId,
        String ipAddress,
        String userAgent,
        String detailsJson,
        Instant createdAt
) {
}
