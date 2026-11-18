package com.sellerintelligence.modules.reporting;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade tenant-scoped representando documentos gerados, relatórios ou exportações de dados.
 * Protegida por integridade referencial estrita e PostgreSQL Row-Level Security (RLS).
 */
@Entity
@Table(name = "tenant_documents")
public class TenantDocument {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "checksum", nullable = false, length = 64)
    private String checksum;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TenantDocument() {
        // JPA requirement
    }

    public TenantDocument(
            UUID id,
            UUID tenantId,
            String fileName,
            String storagePath,
            Long fileSizeBytes,
            String contentType,
            String checksum
    ) {
        this.id = Objects.requireNonNull(id, "id não pode ser nulo");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        this.fileName = Objects.requireNonNull(fileName, "fileName não pode ser nulo");
        this.storagePath = Objects.requireNonNull(storagePath, "storagePath não pode ser nulo");
        this.fileSizeBytes = Objects.requireNonNull(fileSizeBytes, "fileSizeBytes não pode ser nulo");
        this.contentType = Objects.requireNonNull(contentType, "contentType não pode ser nulo");
        this.checksum = Objects.requireNonNull(checksum, "checksum não pode ser nulo");
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public String getContentType() {
        return contentType;
    }

    public String getChecksum() {
        return checksum;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TenantDocument that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
