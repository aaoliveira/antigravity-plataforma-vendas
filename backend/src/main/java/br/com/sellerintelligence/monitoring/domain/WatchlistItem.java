package br.com.sellerintelligence.monitoring.domain;

import br.com.sellerintelligence.tenancy.store.Marketplace;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "monitoring_watchlist_items")
public class WatchlistItem {

    @Id
    private UUID id;

    @Column(name = "watchlist_id", nullable = false)
    private UUID watchlistId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "marketplace", nullable = false, length = 50)
    private Marketplace marketplace;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 50)
    private WatchlistItemType itemType;

    @Column(name = "external_id", nullable = false, length = 150)
    private String externalId;

    @Column(name = "target_name", nullable = false, length = 500)
    private String targetName;

    @Column(name = "target_url", columnDefinition = "TEXT")
    private String targetUrl;

    @Column(name = "seller_nickname", length = 255)
    private String sellerNickname;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private WatchlistItemStatus status;

    @Column(name = "check_frequency_minutes", nullable = false)
    private int checkFrequencyMinutes;

    @Column(name = "last_collected_at")
    private Instant lastCollectedAt;

    @Column(name = "last_status", length = 50)
    private String lastStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WatchlistItem() {}

    public WatchlistItem(UUID watchlistId, UUID tenantId, Marketplace marketplace, WatchlistItemType itemType,
                        String externalId, String targetName, String targetUrl, String sellerNickname,
                        int checkFrequencyMinutes) {
        this.id = UUID.randomUUID();
        this.watchlistId = Objects.requireNonNull(watchlistId, "watchlistId cannot be null");
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId cannot be null");
        this.marketplace = Objects.requireNonNull(marketplace, "marketplace cannot be null");
        this.itemType = Objects.requireNonNull(itemType, "itemType cannot be null");
        this.externalId = Objects.requireNonNull(externalId, "externalId cannot be null").trim();
        this.targetName = Objects.requireNonNull(targetName, "targetName cannot be null").trim();
        this.targetUrl = targetUrl;
        this.sellerNickname = sellerNickname;
        this.status = WatchlistItemStatus.ACTIVE;
        this.checkFrequencyMinutes = checkFrequencyMinutes > 0 ? checkFrequencyMinutes : 360;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getWatchlistId() {
        return watchlistId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public Marketplace getMarketplace() {
        return marketplace;
    }

    public WatchlistItemType getItemType() {
        return itemType;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public String getSellerNickname() {
        return sellerNickname;
    }

    public WatchlistItemStatus getStatus() {
        return status;
    }

    public int getCheckFrequencyMinutes() {
        return checkFrequencyMinutes;
    }

    public Instant getLastCollectedAt() {
        return lastCollectedAt;
    }

    public String getLastStatus() {
        return lastStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void untrack() {
        this.status = WatchlistItemStatus.UNTRACKED;
        this.updatedAt = Instant.now();
    }

    public void pause() {
        this.status = WatchlistItemStatus.PAUSED;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.status = WatchlistItemStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void recordCollection(Instant collectedAt, String lastStatus) {
        this.lastCollectedAt = collectedAt;
        this.lastStatus = lastStatus;
        this.updatedAt = Instant.now();
    }
}
