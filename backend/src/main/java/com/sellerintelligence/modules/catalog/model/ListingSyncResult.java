package com.sellerintelligence.modules.catalog.model;

import java.util.UUID;

/**
 * Resumo do resultado da execução da sincronização de anúncios próprios.
 */
public record ListingSyncResult(
        UUID sellerAccountId,
        String marketplace,
        int syncedCount,
        int createdCount,
        int updatedCount,
        String status,
        int lastOffset,
        String errorMessage
) {
}
