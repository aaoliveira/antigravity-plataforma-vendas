package br.com.sellerintelligence.ai.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "listing_versions")
public class ListingVersion {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "listing_id")
    private UUID listingId;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "attributes_json", columnDefinition = "TEXT")
    private String attributesJson;

    @Column(name = "status", nullable = false, length = 50)
    private String status; // DRAFT, ACCEPTED, REJECTED

    @Column(name = "source", nullable = false, length = 50)
    private String source; // AI_GENERATED, MANUAL_EDIT

    @Column(name = "prompt_version", length = 50)
    private String promptVersion;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "created_by", length = 150)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ListingVersion() {
    }

    public ListingVersion(
            UUID tenantId,
            UUID listingId,
            UUID productId,
            Integer versionNumber,
            String title,
            String description,
            String attributesJson,
            String status,
            String source,
            String promptVersion,
            String modelName,
            String createdBy
    ) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.listingId = listingId;
        this.productId = productId;
        this.versionNumber = versionNumber;
        this.title = title;
        this.description = description;
        this.attributesJson = attributesJson;
        this.status = status != null ? status : "DRAFT";
        this.source = source != null ? source : "AI_GENERATED";
        this.promptVersion = promptVersion;
        this.modelName = modelName;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getListingId() {
        return listingId;
    }

    public UUID getProductId() {
        return productId;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        this.updatedAt = Instant.now();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = Instant.now();
    }

    public String getAttributesJson() {
        return attributesJson;
    }

    public void setAttributesJson(String attributesJson) {
        this.attributesJson = attributesJson;
        this.updatedAt = Instant.now();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public String getSource() {
        return source;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public String getModelName() {
        return modelName;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
