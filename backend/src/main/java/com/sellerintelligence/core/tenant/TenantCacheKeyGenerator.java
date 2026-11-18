package com.sellerintelligence.core.tenant;

import java.util.Objects;
import java.util.UUID;

/**
 * Gerador determinístico de chaves de cache (Redis) namespaced por tenant.
 * Previne categoricamente colisões de chaves entre diferentes organizações.
 *
 * Formato padrão: tenant:{tenantId}:{entity}:{identifier}
 */
public final class TenantCacheKeyGenerator {

    private TenantCacheKeyGenerator() {
    }

    /**
     * Gera a chave de cache padronizada namespaced para um tenant específico.
     *
     * @param tenantId UUID do tenant
     * @param entity nome do domínio/entidade (ex: "connections", "reports")
     * @param identifier identificador único do recurso (ex: ID, slug, data)
     * @return chave estruturada tenant:{tenantId}:{entity}:{identifier}
     */
    public static String generateKey(UUID tenantId, String entity, String identifier) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo para geração de chave de cache");
        if (entity == null || entity.isBlank()) {
            throw new IllegalArgumentException("entity não pode ser nula ou vazia");
        }
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("identifier não pode ser nulo ou vazio");
        }

        String sanitizedEntity = entity.trim().toLowerCase().replaceAll("[^a-z0-9_-]", "_");
        String sanitizedIdentifier = identifier.trim().replaceAll("\\s+", "_");

        return String.format("tenant:%s:%s:%s", tenantId, sanitizedEntity, sanitizedIdentifier);
    }

    /**
     * Gera a chave utilizando o tenantId presente no TenantContextHolder atual.
     */
    public static String generateKeyForCurrentTenant(String entity, String identifier) {
        UUID tenantId = TenantContextHolder.getRequiredTenantId();
        return generateKey(tenantId, entity, identifier);
    }
}
