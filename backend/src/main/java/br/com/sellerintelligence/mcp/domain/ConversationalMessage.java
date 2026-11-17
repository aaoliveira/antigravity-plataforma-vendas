package br.com.sellerintelligence.mcp.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "conversational_messages")
public class ConversationalMessage {

    @Id
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private ChatRole role;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "tool_calls", columnDefinition = "TEXT")
    private String toolCalls;

    @Column(name = "provenance_metadata", columnDefinition = "TEXT")
    private String provenanceMetadata;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected ConversationalMessage() {}

    public ConversationalMessage(UUID sessionId, UUID tenantId, ChatRole role, String content,
                                 String toolCalls, String provenanceMetadata) {
        this.id = UUID.randomUUID();
        this.sessionId = sessionId;
        this.tenantId = tenantId;
        this.role = role;
        this.content = content != null ? content : "";
        this.toolCalls = toolCalls;
        this.provenanceMetadata = provenanceMetadata;
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getSessionId() { return sessionId; }
    public UUID getTenantId() { return tenantId; }
    public ChatRole getRole() { return role; }
    public String getContent() { return content; }
    public String getToolCalls() { return toolCalls; }
    public String getProvenanceMetadata() { return provenanceMetadata; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
