package br.com.sellerintelligence.monitoring.web;

import br.com.sellerintelligence.monitoring.domain.*;
import br.com.sellerintelligence.monitoring.service.*;
import br.com.sellerintelligence.tenancy.domain.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/monitoring")
public class MonitoringController {

    private final MonitoringWatchlistService watchlistService;
    private final MonitoringCollectorService collectorService;
    private final MonitoringTimelineService timelineService;
    private final MonitoringAlertService alertService;
    private final CompetitorComparisonService comparisonService;

    public MonitoringController(MonitoringWatchlistService watchlistService,
                                MonitoringCollectorService collectorService,
                                MonitoringTimelineService timelineService,
                                MonitoringAlertService alertService,
                                CompetitorComparisonService comparisonService) {
        this.watchlistService = watchlistService;
        this.collectorService = collectorService;
        this.timelineService = timelineService;
        this.alertService = alertService;
        this.comparisonService = comparisonService;
    }

    // 1. Watchlists
    @PostMapping("/watchlists")
    public ResponseEntity<Watchlist> createWatchlist(@Valid @RequestBody CreateWatchlistRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        Watchlist watchlist = watchlistService.createWatchlist(
                tenantId, request.name(), request.description(), request.type()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(watchlist);
    }

    @GetMapping("/watchlists")
    public ResponseEntity<List<Watchlist>> listWatchlists() {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(watchlistService.listWatchlists(tenantId));
    }

    @GetMapping("/watchlists/{id}")
    public ResponseEntity<Watchlist> getWatchlist(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        return watchlistService.getWatchlist(id, tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/watchlists/{id}")
    public ResponseEntity<Void> archiveWatchlist(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        watchlistService.archiveWatchlist(id, tenantId);
        return ResponseEntity.noContent().build();
    }

    // 2. Watchlist Items
    @PostMapping("/watchlists/{watchlistId}/items")
    public ResponseEntity<WatchlistItem> addItem(@PathVariable UUID watchlistId,
                                                @Valid @RequestBody AddWatchlistItemRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        WatchlistItem item = watchlistService.addItem(
                watchlistId, tenantId, request.marketplace(), request.itemType(),
                request.externalId(), request.targetName(), request.targetUrl(),
                request.sellerNickname(),
                request.checkFrequencyMinutes() != null ? request.checkFrequencyMinutes() : 360
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    @GetMapping("/watchlists/{watchlistId}/items")
    public ResponseEntity<List<WatchlistItem>> listItems(@PathVariable UUID watchlistId) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(watchlistService.listItems(watchlistId, tenantId));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> untrackItem(@PathVariable UUID itemId) {
        UUID tenantId = TenantContext.getTenantId();
        watchlistService.untrackItem(itemId, tenantId);
        return ResponseEntity.noContent().build();
    }

    // 3. Snapshots & Collection
    @PostMapping("/items/{itemId}/snapshot")
    public ResponseEntity<MonitoringSnapshot> recordSnapshot(@PathVariable UUID itemId,
                                                            @RequestBody RecordSnapshotRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        WatchlistItem item = watchlistService.getItem(itemId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Watchlist item not found: " + itemId));

        MonitoringSnapshot snapshot = collectorService.recordObservation(
                tenantId, itemId, item.getMarketplace(), item.getExternalId(),
                request.price(), request.originalPrice(),
                request.listingStatus() != null ? request.listingStatus() : "ACTIVE",
                request.title() != null ? request.title() : item.getTargetName(),
                request.sellerNickname() != null ? request.sellerNickname() : item.getSellerNickname(),
                request.sellerReputation(),
                request.freeShipping(),
                request.thumbnailUrl(),
                request.source() != null ? request.source() : "MANUAL_OR_CONNECTOR",
                request.collectedAt()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(snapshot);
    }

    // 4. Timeline with Explicit Gaps (Never Interpolate)
    @GetMapping("/items/{itemId}/timeline")
    public ResponseEntity<MonitoringTimelineService.TimelineReport> getTimeline(@PathVariable UUID itemId) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(timelineService.buildTimeline(itemId, tenantId));
    }

    // 5. Alert Rules & Alerts
    @PostMapping("/rules")
    public ResponseEntity<MonitoringAlertRule> createRule(@Valid @RequestBody CreateAlertRuleRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        MonitoringAlertRule rule = alertService.createRule(
                tenantId, request.watchlistId(), request.name(), request.eventType(),
                request.minPriceDeltaPercentage(), request.direction(),
                request.cooldownMinutes() != null ? request.cooldownMinutes() : 1440,
                request.channel()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(rule);
    }

    @GetMapping("/rules")
    public ResponseEntity<List<MonitoringAlertRule>> listRules() {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(alertService.listRules(tenantId));
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<MonitoringAlert>> listAlerts() {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(alertService.listAlerts(tenantId));
    }

    @PatchMapping("/alerts/{id}/read")
    public ResponseEntity<Void> markAlertAsRead(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        boolean updated = alertService.markAlertAsRead(id, tenantId);
        return updated ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    // 6. Competitor vs Own Listing Comparator
    public record CompareRequest(UUID ownListingId, UUID competitorItemId) {}

    @PostMapping("/compare")
    public ResponseEntity<CompetitorComparisonService.ComparisonResult> compare(@RequestBody CompareRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(comparisonService.compare(request.ownListingId(), request.competitorItemId(), tenantId));
    }
}
