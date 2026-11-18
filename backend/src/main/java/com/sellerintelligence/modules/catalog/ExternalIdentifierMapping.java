package com.sellerintelligence.modules.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Mapeia identificadores externos de marketplaces (item_id, variation_id, sku externo, gtin)
 * para entidades canônicas internas (produto, variante ou listing) com garantia de isolamento por tenant.
 */
@Entity
@Table(name = "external_identifier_mappings")
public class ExternalIdentifierMapping {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "marketplace", nullable = false, length = 50)
    private String marketplace;

    @Column(name = "external_id_type", nullable = false, length = 50)
    private String externalIdType;

    @Column(name = "external_id_value", nullable = false, length = 100)
    private String externalIdValue;

    @Column(name = "internal_entity_type", nullable = false, length = 50)
    private String internalEntityType;

    @Column(name = "internal_entity_id", nullable = false)
    private UUID internalEntityId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ExternalIdentifierMapping() {
        // JPA requirement
    }

    public ExternalIdentifierMapping(
            UUID id,
            UUID tenantId,
            String marketplace,
            String externalIdType,
            String externalIdValue,
            String internalEntityType,
            UUID internalEntityId
    ) {
        this.id = Objects.requireNonNull(id, "id não pode ser nulo");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        this.marketplace = Objects.requireNonNull(marketplace, "marketplace não pode ser nulo");
        this.externalIdType = Objects.requireNonNull(externalIdType, "externalIdType não pode ser nulo");
        this.externalIdValue = Objects.requireNonNull(externalIdValue, "externalIdValue não pode ser nulo");
        this.internalEntityType = Objects.requireNonNull(internalEntityType, "internalEntityType não pode ser nulo");
        this.internalEntityId = Objects.requireNonNull(internalEntityId, "internalEntityId não pode ser nulo");
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getMarketplace() {
        return marketplace;
    }

    public String getExternalIdType() {
        return externalIdType;
    }

    public String getExternalIdValue() {
        return externalIdValue;
    }

    public String getInternalEntityType() {
        return internalEntityType;
    }

    public UUID getInternalEntityId() {
        return internalEntityId;
    }

    public void setInternalEntityId(UUID internalEntityId) {
        this.internalEntityId = Objects.requireNonNull(internalEntityId, "internalEntityId não pode ser nulo");
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExternalIdentifierMapping that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
