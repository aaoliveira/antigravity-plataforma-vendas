package com.sellerintelligence.modules.reporting.model;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO de resposta com detalhes do job de exportação e URL temporária de download.
 */
public record ExportJobResponseDto(
        UUID id,
        UUID tenantId,
        String requestedBy,
        String idempotencyKey,
        ExportDataset dataset,
        ExportFileFormat fileFormat,
        String filterParamsJson,
        ExportJobStatus status,
        int maxRows,
        int totalRows,
        boolean isTruncated,
        String fileName,
        Long fileSizeBytes,
        String checksumSha256,
        String downloadUrl,
        String downloadToken,
        Instant expiresAt,
        Instant completedAt,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt
) {
}
