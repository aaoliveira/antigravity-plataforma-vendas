package br.com.sellerintelligence.discovery.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "discovery_visual_searches")
public class DiscoveryVisualSearch {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "stored_image_path", nullable = false, length = 500)
    private String storedImagePath;

    @Column(name = "image_mime_type", nullable = false, length = 100)
    private String imageMimeType;

    @Column(name = "search_query_description", length = 500)
    private String searchQueryDescription;

    @Column(name = "top_matches_count", nullable = false)
    private int topMatchesCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DiscoveryVisualSearch() {}

    public DiscoveryVisualSearch(UUID tenantId, String storedImagePath, String imageMimeType,
                                 String searchQueryDescription, int topMatchesCount) {
        this.id = UUID.randomUUID();
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId cannot be null");
        this.storedImagePath = Objects.requireNonNull(storedImagePath, "storedImagePath cannot be null");
        this.imageMimeType = Objects.requireNonNull(imageMimeType, "imageMimeType cannot be null");
        this.searchQueryDescription = searchQueryDescription;
        this.topMatchesCount = topMatchesCount;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getStoredImagePath() { return storedImagePath; }
    public String getImageMimeType() { return imageMimeType; }
    public String getSearchQueryDescription() { return searchQueryDescription; }
    public int getTopMatchesCount() { return topMatchesCount; }
    public Instant getCreatedAt() { return createdAt; }
}
