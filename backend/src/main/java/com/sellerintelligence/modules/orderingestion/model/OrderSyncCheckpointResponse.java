package com.sellerintelligence.modules.orderingestion.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Resposta de status de checkpoint de sincronização de pedidos.
 */
public record OrderSyncCheckpointResponse(
        UUID id,
        UUID tenantId,
        UUID sellerAccountId,
        String marketplace,
        String syncStatus,
        String cursorToken,
        int lastOffset,
        int pageSize,
        int totalOrdersSynced,
        Instant startedAt,
        Instant lastCheckpointAt,
        Instant completedAt,
        String errorMessage
) {}
