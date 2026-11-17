package br.com.sellerintelligence.mcp.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "mcp_tool_execution_logs")
public class McpToolExecutionLog {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "api_key_id")
    private UUID apiKeyId;

    @Column(name = "tool_name", nullable = false, length = 100)
    private String toolName;

    @Column(name = "arguments_sanitized", nullable = false, columnDefinition = "TEXT")
    private String argumentsSanitized;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_status", nullable = false, length = 50)
    private McpExecutionStatus executionStatus;

    @Column(name = "latency_ms", nullable = false)
    private Long latencyMs;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "prompt_version", length = 50)
    private String promptVersion;

    @Column(name = "result_summary", columnDefinition = "TEXT")
    private String resultSummary;

    @Column(name = "provenance_source", length = 100)
    private String provenanceSource;

    @Column(name = "provenance_classification", length = 50)
    private String provenanceClassification;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected McpToolExecutionLog() {}

    public McpToolExecutionLog(UUID tenantId, UUID userId, UUID apiKeyId, String toolName,
                               String argumentsSanitized, McpExecutionStatus executionStatus,
                               Long latencyMs, String modelName, String promptVersion,
                               String resultSummary, String provenanceSource,
                               String provenanceClassification, String ipAddress) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.userId = userId;
        this.apiKeyId = apiKeyId;
        this.toolName = toolName;
        this.argumentsSanitized = argumentsSanitized != null ? argumentsSanitized : "{}";
        this.executionStatus = executionStatus;
        this.latencyMs = latencyMs != null ? latencyMs : 0L;
        this.modelName = modelName;
        this.promptVersion = promptVersion;
        this.resultSummary = resultSummary;
        this.provenanceSource = provenanceSource;
        this.provenanceClassification = provenanceClassification;
        this.ipAddress = ipAddress;
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getUserId() { return userId; }
    public UUID getApiKeyId() { return apiKeyId; }
    public String getToolName() { return toolName; }
    public String getArgumentsSanitized() { return argumentsSanitized; }
    public McpExecutionStatus getExecutionStatus() { return executionStatus; }
    public Long getLatencyMs() { return latencyMs; }
    public String getModelName() { return modelName; }
    public String getPromptVersion() { return promptVersion; }
    public String getResultSummary() { return resultSummary; }
    public String getProvenanceSource() { return provenanceSource; }
    public String getProvenanceClassification() { return provenanceClassification; }
    public String getIpAddress() { return ipAddress; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
