package com.sellerintelligence.modules.aiassistant.draft.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade que representa o rascunho de anúncio gerado por IA para um canal (P020-BE).
 * Todo conteúdo é estritamente um rascunho sob governança do tenant. Nenhuma publicação externa é permitida no MVP.
 */
@Entity
@Table(name = "draft_listings")
public class DraftListing {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "product_variant_id")
    private UUID productVariantId;

    @Column(name = "target_marketplace", nullable = false, length = 50)
    private String targetMarketplace;

    @Column(name = "current_version_number", nullable = false)
    private Integer currentVersionNumber;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DraftListing() {
        // JPA requirement
    }

    public DraftListing(
            UUID id,
            UUID tenantId,
            UUID productId,
            UUID productVariantId,
            String targetMarketplace,
            Integer currentVersionNumber,
            String status
    ) {
        this.id = Objects.requireNonNull(id, "id não pode ser nulo");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        this.productId = productId;
        this.productVariantId = productVariantId;
        this.targetMarketplace = Objects.requireNonNull(targetMarketplace, "targetMarketplace não pode ser nulo");
        this.currentVersionNumber = currentVersionNumber != null ? currentVersionNumber : 1;
        this.status = status != null ? status : "DRAFT";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
        this.updatedAt = Instant.now();
    }

    public UUID getProductVariantId() {
        return productVariantId;
    }

    public void setProductVariantId(UUID productVariantId) {
        this.productVariantId = productVariantId;
        this.updatedAt = Instant.now();
    }

    public String getTargetMarketplace() {
        return targetMarketplace;
    }

    public void setTargetMarketplace(String targetMarketplace) {
        this.targetMarketplace = Objects.requireNonNull(targetMarketplace, "targetMarketplace não pode ser nulo");
        this.updatedAt = Instant.now();
    }

    public Integer getCurrentVersionNumber() {
        return currentVersionNumber;
    }

    public void setCurrentVersionNumber(Integer currentVersionNumber) {
        this.currentVersionNumber = currentVersionNumber;
        this.updatedAt = Instant.now();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = Objects.requireNonNull(status, "status não pode ser nulo");
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
