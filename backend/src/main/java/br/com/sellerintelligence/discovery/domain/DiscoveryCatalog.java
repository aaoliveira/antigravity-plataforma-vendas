package br.com.sellerintelligence.discovery.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "discovery_catalogs")
public class DiscoveryCatalog {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "filename", nullable = false)
    private String filename;

    @Column(name = "stored_filename", nullable = false)
    private String storedFilename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size_bytes", nullable = false)
    private long fileSizeBytes;

    @Column(name = "supplier_name")
    private String supplierName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private CatalogStatus status;

    @Column(name = "total_items", nullable = false)
    private int totalItems;

    @Column(name = "reviewed_items", nullable = false)
    private int reviewedItems;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DiscoveryCatalog() {}

    public DiscoveryCatalog(UUID tenantId, String filename, String storedFilename,
                            String contentType, long fileSizeBytes, String supplierName) {
        this.id = UUID.randomUUID();
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId cannot be null");
        this.filename = Objects.requireNonNull(filename, "filename cannot be null");
        this.storedFilename = Objects.requireNonNull(storedFilename, "storedFilename cannot be null");
        this.contentType = Objects.requireNonNull(contentType, "contentType cannot be null");
        this.fileSizeBytes = fileSizeBytes;
        this.supplierName = supplierName;
        this.status = CatalogStatus.UPLOADED;
        this.totalItems = 0;
        this.reviewedItems = 0;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void markParsed(int totalItems) {
        this.status = CatalogStatus.PARSED;
        this.totalItems = totalItems;
        this.updatedAt = Instant.now();
    }

    public void markFailed(String reason) {
        this.status = CatalogStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = Instant.now();
    }

    public void incrementReviewedItems() {
        this.reviewedItems++;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getFilename() { return filename; }
    public String getStoredFilename() { return storedFilename; }
    public String getContentType() { return contentType; }
    public long getFileSizeBytes() { return fileSizeBytes; }
    public String getSupplierName() { return supplierName; }
    public CatalogStatus getStatus() { return status; }
    public int getTotalItems() { return totalItems; }
    public int getReviewedItems() { return reviewedItems; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
