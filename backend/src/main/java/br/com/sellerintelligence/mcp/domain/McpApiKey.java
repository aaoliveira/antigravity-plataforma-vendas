package br.com.sellerintelligence.mcp.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "mcp_api_keys")
public class McpApiKey {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "key_hash", nullable = false, unique = true, length = 128)
    private String keyHash;

    @Column(name = "key_prefix", nullable = false, length = 16)
    private String keyPrefix;

    @Column(name = "permissions", nullable = false, length = 255)
    private String permissions = "READ_ONLY";

    @Column(name = "status", nullable = false, length = 50)
    private String status = "ACTIVE";

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "last_used_at")
    private OffsetDateTime lastUsedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected McpApiKey() {}

    public McpApiKey(UUID tenantId, String name, String keyHash, String keyPrefix, UUID createdBy, OffsetDateTime expiresAt) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.name = name;
        this.keyHash = keyHash;
        this.keyPrefix = keyPrefix;
        this.permissions = "READ_ONLY";
        this.status = "ACTIVE";
        this.createdBy = createdBy;
        this.expiresAt = expiresAt;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getName() { return name; }
    public String getKeyHash() { return keyHash; }
    public String getKeyPrefix() { return keyPrefix; }
    public String getPermissions() { return permissions; }
    public String getStatus() { return status; }
    public UUID getCreatedBy() { return createdBy; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public OffsetDateTime getLastUsedAt() { return lastUsedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public boolean isActive() {
        if (!"ACTIVE".equalsIgnoreCase(this.status)) {
            return false;
        }
        return expiresAt == null || expiresAt.isAfter(OffsetDateTime.now());
    }

    public void markUsed() {
        this.lastUsedAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public void revoke() {
        this.status = "REVOKED";
        this.updatedAt = OffsetDateTime.now();
    }
}
