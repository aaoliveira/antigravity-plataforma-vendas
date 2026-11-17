package br.com.sellerintelligence.monitoring.service;

import br.com.sellerintelligence.monitoring.domain.*;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitoringTimelineServiceTest {

    @Mock
    private MonitoringSnapshotRepository snapshotRepository;

    @Mock
    private WatchlistItemRepository itemRepository;

    private MonitoringTimelineService timelineService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID itemId = UUID.randomUUID();
    private final UUID watchlistId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        timelineService = new MonitoringTimelineService(snapshotRepository, itemRepository);
    }

    @Test
    @DisplayName("Should build timeline with explicit GAP point when collection interval exceeds threshold without interpolating price")
    void buildTimelineWithExplicitGap() {
        WatchlistItem item = new WatchlistItem(
                watchlistId, tenantId, Marketplace.MERCADO_LIVRE, WatchlistItemType.LISTING,
                "MLB12345", "Monitor Gamer 24", null, "seller_pro", 360 // expected every 6 hours
        );

        when(itemRepository.findByIdAndTenantId(itemId, tenantId)).thenReturn(Optional.of(item));

        // Day 1 at 10:00 (Price 1000)
        Instant t1 = Instant.parse("2026-11-01T10:00:00Z");
        // Day 4 at 10:00 (Price 900) -> 72 hours later! Allowed threshold is 9 hours (360 * 1.5).
        Instant t2 = Instant.parse("2026-11-04T10:00:00Z");

        MonitoringSnapshot s1 = new MonitoringSnapshot(
                tenantId, itemId, Marketplace.MERCADO_LIVRE, "MLB12345",
                new BigDecimal("1000.00"), null, "BRL", "ACTIVE",
                "Monitor Gamer 24", "seller_pro", "Platinum", true,
                null, CollectionStatus.SUCCESS, null, "API", "OBSERVED", t1
        );

        MonitoringSnapshot s2 = new MonitoringSnapshot(
                tenantId, itemId, Marketplace.MERCADO_LIVRE, "MLB12345",
                new BigDecimal("900.00"), null, "BRL", "ACTIVE",
                "Monitor Gamer 24", "seller_pro", "Platinum", true,
                null, CollectionStatus.SUCCESS, null, "API", "OBSERVED", t2
        );

        when(snapshotRepository.findByItemIdAndTenantIdOrderByCollectedAtAsc(itemId, tenantId))
                .thenReturn(List.of(s1, s2));

        MonitoringTimelineService.TimelineReport report = timelineService.buildTimeline(itemId, tenantId);

        assertThat(report).isNotNull();
        assertThat(report.totalObservations()).isEqualTo(2);
        assertThat(report.detectedGaps()).isEqualTo(1);
        assertThat(report.points()).hasSize(3); // s1, GAP, s2

        // Verify GAP point
        MonitoringTimelineService.TimelinePoint gapPoint = report.points().get(1);
        assertThat(gapPoint.isGap()).isTrue();
        assertThat(gapPoint.collectionStatus()).isEqualTo(CollectionStatus.GAP);
        // Anti-hallucination verification: price MUST be null (never interpolated)
        assertThat(gapPoint.price()).isNull();
        assertThat(gapPoint.provenance()).isEqualTo("UNAVAILABLE");
        assertThat(gapPoint.gapDurationMinutes()).isEqualTo(Duration.between(t1, t2).toMinutes());
    }

    @Test
    @DisplayName("Should record COLLECTION_FAILED point in timeline and decrease coverage percentage")
    void recordCollectionFailedInTimeline() {
        WatchlistItem item = new WatchlistItem(
                watchlistId, tenantId, Marketplace.MERCADO_LIVRE, WatchlistItemType.LISTING,
                "MLB12345", "Item", null, "seller", 360
        );
        when(itemRepository.findByIdAndTenantId(itemId, tenantId)).thenReturn(Optional.of(item));

        Instant t1 = Instant.parse("2026-11-17T08:00:00Z");
        Instant t2 = Instant.parse("2026-11-17T12:00:00Z");

        MonitoringSnapshot successSnap = new MonitoringSnapshot(
                tenantId, itemId, Marketplace.MERCADO_LIVRE, "MLB12345",
                new BigDecimal("50.00"), null, "BRL", "ACTIVE",
                "Item", "seller", null, null, null, CollectionStatus.SUCCESS, null, "API", "OBSERVED", t1
        );

        MonitoringSnapshot failSnap = MonitoringSnapshot.failure(
                tenantId, itemId, Marketplace.MERCADO_LIVRE, "MLB12345", "429 Rate Limited by Marketplace", t2
        );

        when(snapshotRepository.findByItemIdAndTenantIdOrderByCollectedAtAsc(itemId, tenantId))
                .thenReturn(List.of(successSnap, failSnap));

        MonitoringTimelineService.TimelineReport report = timelineService.buildTimeline(itemId, tenantId);

        assertThat(report.successfulObservations()).isEqualTo(1);
        assertThat(report.failedObservations()).isEqualTo(1);
        assertThat(report.coveragePercentage()).isEqualByComparingTo("50.00");

        MonitoringTimelineService.TimelinePoint failPoint = report.points().get(1);
        assertThat(failPoint.collectionStatus()).isEqualTo(CollectionStatus.COLLECTION_FAILED);
        assertThat(failPoint.failureReason()).contains("Rate Limited");
    }
}
