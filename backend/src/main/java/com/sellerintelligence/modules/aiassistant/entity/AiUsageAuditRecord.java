package com.sellerintelligence.modules.aiassistant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entidade JPA para auditoria de consumo de IA, tokens, latência, custos e defesas de segurança.
 * Protegida por Row-Level Security (RLS) no PostgreSQL 17.
 */
@Entity
@Table(name = "ai_usage_audits")
public class AiUsageAuditRecord {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "prompt_key", nullable = false, length = 100)
    private String promptKey;

    @Column(name = "prompt_version", nullable = false, length = 20)
    private String promptVersion;

    @Column(name = "prompt_tokens", nullable = false)
    private int promptTokens;

    @Column(name = "completion_tokens", nullable = false)
    private int completionTokens;

    @Column(name = "total_tokens", nullable = false)
    private int totalTokens;

    @Column(name = "latency_ms", nullable = false)
    private long latencyMs;

    @Column(name = "estimated_cost_brl", nullable = false, precision = 10, scale = 6)
    private BigDecimal estimatedCostBrl;

    @Column(name = "tools_invoked", length = 500)
    private String toolsInvoked;

    @Column(name = "injection_detected", nullable = false)
    private boolean injectionDetected;

    @Column(name = "injection_threat_type", length = 100)
    private String injectionThreatType;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public AiUsageAuditRecord() {
    }

    public AiUsageAuditRecord(
            UUID id,
            UUID tenantId,
            UUID userId,
            String provider,
            String modelName,
            String promptKey,
            String promptVersion,
            int promptTokens,
            int completionTokens,
            int totalTokens,
            long latencyMs,
            BigDecimal estimatedCostBrl,
            String toolsInvoked,
            boolean injectionDetected,
            String injectionThreatType,
            String status,
            String errorCode,
            String metadataJson,
            Instant createdAt
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.userId = userId;
        this.provider = provider;
        this.modelName = modelName;
        this.promptKey = promptKey;
        this.promptVersion = promptVersion;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
        this.latencyMs = latencyMs;
        this.estimatedCostBrl = estimatedCostBrl != null ? estimatedCostBrl : BigDecimal.ZERO;
        this.toolsInvoked = toolsInvoked;
        this.injectionDetected = injectionDetected;
        this.injectionThreatType = injectionThreatType;
        this.status = status;
        this.errorCode = errorCode;
        this.metadataJson = metadataJson;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getProvider() {
        return provider;
    }

    public String getModelName() {
        return modelName;
    }

    public String getPromptKey() {
        return promptKey;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public int getPromptTokens() {
        return promptTokens;
    }

    public int getCompletionTokens() {
        return completionTokens;
    }

    public int getTotalTokens() {
        return totalTokens;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public BigDecimal getEstimatedCostBrl() {
        return estimatedCostBrl;
    }

    public String getToolsInvoked() {
        return toolsInvoked;
    }

    public boolean isInjectionDetected() {
        return injectionDetected;
    }

    public String getInjectionThreatType() {
        return injectionThreatType;
    }

    public String getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
