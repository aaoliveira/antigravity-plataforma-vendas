package com.sellerintelligence.modules.orderingestion.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Resposta de registro de falha na Dead-Letter Queue (DLQ).
 */
public record OrderDlqRecordResponse(
        UUID id,
        UUID tenantId,
        UUID sellerAccountId,
        String marketplace,
        String externalOrderId,
        String rawPayloadJson,
        String errorReason,
        int retryCount,
        String status,
        Instant createdAt,
        Instant updatedAt
) {}
