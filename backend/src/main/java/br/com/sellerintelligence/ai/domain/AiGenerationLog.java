package br.com.sellerintelligence.ai.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_generation_logs")
public class AiGenerationLog {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", length = 150)
    private String userId;

    @Column(name = "listing_id")
    private UUID listingId;

    @Column(name = "prompt_type", nullable = false, length = 100)
    private String promptType;

    @Column(name = "prompt_version", nullable = false, length = 50)
    private String promptVersion;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "latency_ms", nullable = false)
    private Long latencyMs;

    @Column(name = "status", nullable = false, length = 50)
    private String status; // SUCCESS, ERROR, FALLBACK

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AiGenerationLog() {
    }

    public AiGenerationLog(
            UUID tenantId,
            String userId,
            UUID listingId,
            String promptType,
            String promptVersion,
            String modelName,
            Long latencyMs,
            String status,
            String errorCode
    ) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.userId = userId;
        this.listingId = listingId;
        this.promptType = promptType;
        this.promptVersion = promptVersion;
        this.modelName = modelName;
        this.latencyMs = latencyMs;
        this.status = status;
        this.errorCode = errorCode;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getUserId() {
        return userId;
    }

    public UUID getListingId() {
        return listingId;
    }

    public String getPromptType() {
        return promptType;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public String getModelName() {
        return modelName;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public String getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
