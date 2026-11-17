package br.com.sellerintelligence.monitoring.service;

import br.com.sellerintelligence.monitoring.domain.CollectionStatus;
import br.com.sellerintelligence.monitoring.domain.MonitoringSnapshot;
import br.com.sellerintelligence.monitoring.domain.MonitoringSnapshotRepository;
import br.com.sellerintelligence.monitoring.domain.WatchlistItem;
import br.com.sellerintelligence.monitoring.domain.WatchlistItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class MonitoringTimelineService {

    private final MonitoringSnapshotRepository snapshotRepository;
    private final WatchlistItemRepository itemRepository;

    public MonitoringTimelineService(MonitoringSnapshotRepository snapshotRepository,
                                     WatchlistItemRepository itemRepository) {
        this.snapshotRepository = snapshotRepository;
        this.itemRepository = itemRepository;
    }

    public record TimelinePoint(
            Instant timestamp,
            BigDecimal price,
            BigDecimal originalPrice,
            String listingStatus,
            CollectionStatus collectionStatus,
            boolean isGap,
            Long gapDurationMinutes,
            String provenance,
            String failureReason
    ) {}

    public record TimelineReport(
            UUID itemId,
            String externalId,
            String targetName,
            int checkFrequencyMinutes,
            int totalObservations,
            int successfulObservations,
            int failedObservations,
            int detectedGaps,
            BigDecimal coveragePercentage,
            Instant earliestObservation,
            Instant latestObservation,
            List<TimelinePoint> points
    ) {}

    @Transactional(readOnly = true)
    public TimelineReport buildTimeline(UUID itemId, UUID tenantId) {
        WatchlistItem item = itemRepository.findByIdAndTenantId(itemId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Watchlist item not found: " + itemId));

        List<MonitoringSnapshot> snapshots = snapshotRepository.findByItemIdAndTenantIdOrderByCollectedAtAsc(itemId, tenantId);

        List<TimelinePoint> points = new ArrayList<>();
        int successful = 0;
        int failed = 0;
        int gaps = 0;

        long allowedGapThresholdMinutes = (long) (item.getCheckFrequencyMinutes() * 1.5);
        if (allowedGapThresholdMinutes < 60) {
            allowedGapThresholdMinutes = 60;
        }

        MonitoringSnapshot previous = null;

        for (MonitoringSnapshot snap : snapshots) {
            // Check for gap between previous and current
            if (previous != null) {
                long minutesBetween = Duration.between(previous.getCollectedAt(), snap.getCollectedAt()).toMinutes();
                if (minutesBetween > allowedGapThresholdMinutes) {
                    // Explicit Gap Point — never interpolate prices!
                    Instant gapTime = previous.getCollectedAt().plus(Duration.ofMinutes(minutesBetween / 2));
                    points.add(new TimelinePoint(
                            gapTime,
                            null, // Strict Anti-Hallucination: never fabricate missing prices
                            null,
                            "UNAVAILABLE",
                            CollectionStatus.GAP,
                            true,
                            minutesBetween,
                            "UNAVAILABLE",
                            "Lacuna de coleta detectada: intervalo de " + minutesBetween + " min sem coletas registradas."
                    ));
                    gaps++;
                }
            }

            if (snap.getCollectionStatus() == CollectionStatus.SUCCESS) {
                points.add(new TimelinePoint(
                        snap.getCollectedAt(),
                        snap.getPrice(),
                        snap.getOriginalPrice(),
                        snap.getListingStatus(),
                        CollectionStatus.SUCCESS,
                        false,
                        null,
                        snap.getProvenance(),
                        null
                ));
                successful++;
            } else {
                points.add(new TimelinePoint(
                        snap.getCollectedAt(),
                        null,
                        null,
                        "COLLECTION_FAILED",
                        CollectionStatus.COLLECTION_FAILED,
                        false,
                        null,
                        "UNAVAILABLE",
                        snap.getFailureReason()
                ));
                failed++;
            }

            previous = snap;
        }

        Instant earliest = snapshots.isEmpty() ? null : snapshots.get(0).getCollectedAt();
        Instant latest = snapshots.isEmpty() ? null : snapshots.get(snapshots.size() - 1).getCollectedAt();

        // Calculate Coverage Percentage
        BigDecimal coverage = BigDecimal.valueOf(100.0);
        int totalExpectedOrObserved = successful + failed + gaps;
        if (totalExpectedOrObserved > 0) {
            coverage = BigDecimal.valueOf(successful)
                    .divide(BigDecimal.valueOf(totalExpectedOrObserved), 4, RoundingMode.HALF_EVEN)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_EVEN);
        }

        return new TimelineReport(
                item.getId(),
                item.getExternalId(),
                item.getTargetName(),
                item.getCheckFrequencyMinutes(),
                snapshots.size(),
                successful,
                failed,
                gaps,
                coverage,
                earliest,
                latest,
                points
        );
    }
}
