package com.sellerintelligence.modules.marketplaceadapters.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Contexto de execução de uma chamada a marketplace, contendo tenant, credenciais cifradas
 * e correlation ID para rastreabilidade de observabilidade.
 */
public record ExecutionContext(
        UUID tenantId,
        String sellerAccountId,
        String encryptedAccessToken,
        String correlationId
) {

    public ExecutionContext {
        Objects.requireNonNull(tenantId, "tenantId é obrigatório no ExecutionContext");
        Objects.requireNonNull(sellerAccountId, "sellerAccountId é obrigatório no ExecutionContext");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
    }

    public static ExecutionContext of(UUID tenantId, String sellerAccountId, String encryptedAccessToken) {
        return new ExecutionContext(tenantId, sellerAccountId, encryptedAccessToken, UUID.randomUUID().toString());
    }
}
