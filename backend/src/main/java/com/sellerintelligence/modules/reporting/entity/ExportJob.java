package com.sellerintelligence.modules.reporting.entity;

import com.sellerintelligence.modules.reporting.model.ExportDataset;
import com.sellerintelligence.modules.reporting.model.ExportFileFormat;
import com.sellerintelligence.modules.reporting.model.ExportJobStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade tenant-scoped representando uma solicitação e processamento de exportação.
 */
@Entity
@Table(name = "export_jobs")
public class ExportJob {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "requested_by", nullable = false, length = 100)
    private String requestedBy;

    @Column(name = "idempotency_key", length = 120)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "dataset", nullable = false, length = 50)
    private ExportDataset dataset;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_format", nullable = false, length = 10)
    private ExportFileFormat fileFormat;

    @Column(name = "filter_params_json", nullable = false, columnDefinition = "TEXT")
    private String filterParamsJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ExportJobStatus status;

    @Column(name = "max_rows", nullable = false)
    private int maxRows;

    @Column(name = "total_rows")
    private int totalRows;

    @Column(name = "is_truncated", nullable = false)
    private boolean isTruncated;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "storage_path", length = 500)
    private String storagePath;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "checksum_sha256", length = 64)
    private String checksumSha256;

    @Column(name = "download_token", length = 120)
    private String downloadToken;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ExportJob() {
        // JPA requirement
    }

    public ExportJob(
            UUID id,
            UUID tenantId,
            String requestedBy,
            String idempotencyKey,
            ExportDataset dataset,
            ExportFileFormat fileFormat,
            String filterParamsJson,
            int maxRows
    ) {
        this.id = Objects.requireNonNull(id, "id não pode ser nulo");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        this.requestedBy = Objects.requireNonNull(requestedBy, "requestedBy não pode ser nulo");
        this.idempotencyKey = idempotencyKey;
        this.dataset = Objects.requireNonNull(dataset, "dataset não pode ser nulo");
        this.fileFormat = Objects.requireNonNull(fileFormat, "fileFormat não pode ser nulo");
        this.filterParamsJson = filterParamsJson != null ? filterParamsJson : "{}";
        this.status = ExportJobStatus.PENDING;
        this.maxRows = maxRows;
        this.totalRows = 0;
        this.isTruncated = false;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void markProcessing() {
        this.status = ExportJobStatus.PROCESSING;
        this.updatedAt = Instant.now();
    }

    public void markCompleted(
            String fileName,
            String storagePath,
            long fileSizeBytes,
            String checksumSha256,
            String downloadToken,
            int totalRows,
            boolean isTruncated,
            Instant expiresAt
    ) {
        this.status = ExportJobStatus.COMPLETED;
        this.fileName = fileName;
        this.storagePath = storagePath;
        this.fileSizeBytes = fileSizeBytes;
        this.checksumSha256 = checksumSha256;
        this.downloadToken = downloadToken;
        this.totalRows = totalRows;
        this.isTruncated = isTruncated;
        this.expiresAt = expiresAt;
        this.completedAt = Instant.now();
        this.updatedAt = this.completedAt;
    }

    public void markFailed(String errorMessage) {
        this.status = ExportJobStatus.FAILED;
        this.errorMessage = errorMessage;
        this.updatedAt = Instant.now();
    }

    public void markExpired() {
        this.status = ExportJobStatus.EXPIRED;
        this.updatedAt = Instant.now();
    }

    public boolean isExpired() {
        return this.status == ExportJobStatus.EXPIRED ||
                (this.expiresAt != null && Instant.now().isAfter(this.expiresAt));
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public ExportDataset getDataset() {
        return dataset;
    }

    public ExportFileFormat getFileFormat() {
        return fileFormat;
    }

    public String getFilterParamsJson() {
        return filterParamsJson;
    }

    public ExportJobStatus getStatus() {
        return status;
    }

    public int getMaxRows() {
        return maxRows;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public boolean isTruncated() {
        return isTruncated;
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

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public String getDownloadToken() {
        return downloadToken;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
