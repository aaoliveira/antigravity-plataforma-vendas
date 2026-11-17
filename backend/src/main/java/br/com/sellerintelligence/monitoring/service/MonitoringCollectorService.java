package br.com.sellerintelligence.monitoring.service;

import br.com.sellerintelligence.monitoring.domain.*;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MonitoringCollectorService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringCollectorService.class);

    private final MonitoringSnapshotRepository snapshotRepository;
    private final WatchlistItemRepository itemRepository;
    private final MonitoringDiffEngine diffEngine;
    private final MonitoringAlertService alertService;

    public MonitoringCollectorService(MonitoringSnapshotRepository snapshotRepository,
                                      WatchlistItemRepository itemRepository,
                                      MonitoringDiffEngine diffEngine,
                                      MonitoringAlertService alertService) {
        this.snapshotRepository = snapshotRepository;
        this.itemRepository = itemRepository;
        this.diffEngine = diffEngine;
        this.alertService = alertService;
    }

    @Transactional
    public MonitoringSnapshot recordObservation(UUID tenantId, UUID itemId, Marketplace marketplace,
                                                String externalId, BigDecimal price, BigDecimal originalPrice,
                                                String listingStatus, String title, String sellerNickname,
                                                String sellerReputation, Boolean freeShipping, String thumbnailUrl,
                                                String source, Instant collectedAt) {
        WatchlistItem item = itemRepository.findByIdAndTenantId(itemId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Watchlist item not found: " + itemId));

        // Get the latest prior snapshot for diffing
        Optional<MonitoringSnapshot> previousOpt = snapshotRepository.findFirstByItemIdAndTenantIdOrderByCollectedAtDesc(itemId, tenantId);

        Instant timestamp = collectedAt != null ? collectedAt : Instant.now();
        MonitoringSnapshot snapshot = new MonitoringSnapshot(
                tenantId, itemId, marketplace, externalId,
                price, originalPrice, "BRL", listingStatus,
                title, sellerNickname, sellerReputation, freeShipping,
                thumbnailUrl, CollectionStatus.SUCCESS, null,
                source != null ? source : "OFFICIAL_API",
                "OBSERVED", timestamp
        );

        MonitoringSnapshot saved = snapshotRepository.save(snapshot);
        item.recordCollection(timestamp, listingStatus);
        itemRepository.save(item);

        // Run diff engine
        if (previousOpt.isPresent()) {
            List<MonitoringChangeEvent> changes = diffEngine.detectChanges(previousOpt.get(), saved);
            for (MonitoringChangeEvent change : changes) {
                alertService.evaluateEvent(change, item);
            }
        }

        return saved;
    }

    @Transactional
    public MonitoringSnapshot recordFailure(UUID tenantId, UUID itemId, Marketplace marketplace,
                                            String externalId, String failureReason, Instant collectedAt) {
        WatchlistItem item = itemRepository.findByIdAndTenantId(itemId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Watchlist item not found: " + itemId));

        Instant timestamp = collectedAt != null ? collectedAt : Instant.now();
        MonitoringSnapshot failureSnapshot = MonitoringSnapshot.failure(
                tenantId, itemId, marketplace, externalId, failureReason, timestamp
        );

        MonitoringSnapshot saved = snapshotRepository.save(failureSnapshot);
        item.recordCollection(timestamp, "COLLECTION_FAILED");
        itemRepository.save(item);

        log.warn("Recorded collection failure for item {}: {}", itemId, failureReason);
        return saved;
    }
}
