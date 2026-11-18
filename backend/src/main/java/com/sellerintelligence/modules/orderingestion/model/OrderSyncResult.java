package com.sellerintelligence.modules.orderingestion.model;

import java.util.UUID;

/**
 * Resumo consolidado do resultado de uma sincronização de pedidos.
 */
public record OrderSyncResult(
        UUID tenantId,
        UUID sellerAccountId,
        String marketplace,
        int totalFetched,
        int ordersCreated,
        int ordersUpdated,
        int dlqCount,
        String status
) {}
