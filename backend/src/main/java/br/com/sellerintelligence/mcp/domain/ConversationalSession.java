package br.com.sellerintelligence.mcp.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "conversational_sessions")
public class ConversationalSession {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ConversationalSession() {}

    public ConversationalSession(UUID tenantId, UUID userId, String title) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.userId = userId;
        this.title = title != null && !title.isBlank() ? title : "Nova Conversa";
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getUserId() { return userId; }
    public String getTitle() { return title; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public void updateTitle(String newTitle) {
        this.title = newTitle;
        this.updatedAt = OffsetDateTime.now();
    }
}
