package br.com.sellerintelligence.monitoring.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "monitoring_watchlists")
public class Watchlist {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private WatchlistType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private WatchlistStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Watchlist() {}

    public Watchlist(UUID tenantId, String name, String description, WatchlistType type) {
        this.id = UUID.randomUUID();
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.description = description;
        this.type = type != null ? type : WatchlistType.MIXED;
        this.status = WatchlistStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getName() {
        return name;
    }

    public void update(String name, String description, WatchlistType type) {
        if (name != null && !name.isBlank()) {
            this.name = name.trim();
        }
        this.description = description;
        if (type != null) {
            this.type = type;
        }
        this.updatedAt = Instant.now();
    }

    public void archive() {
        this.status = WatchlistStatus.ARCHIVED;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.status = WatchlistStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public String getDescription() {
        return description;
    }

    public WatchlistType getType() {
        return type;
    }

    public WatchlistStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
