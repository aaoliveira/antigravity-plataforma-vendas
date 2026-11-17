package br.com.sellerintelligence.monitoring.service;

import br.com.sellerintelligence.monitoring.domain.CollectionStatus;
import br.com.sellerintelligence.monitoring.domain.MonitoringSnapshot;
import br.com.sellerintelligence.monitoring.domain.MonitoringSnapshotRepository;
import br.com.sellerintelligence.monitoring.domain.WatchlistItem;
import br.com.sellerintelligence.monitoring.domain.WatchlistItemRepository;
import br.com.sellerintelligence.monitoring.domain.WatchlistItemType;
import br.com.sellerintelligence.sync.domain.Listing;
import br.com.sellerintelligence.sync.domain.ListingRepository;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompetitorComparisonServiceTest {

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private WatchlistItemRepository watchlistItemRepository;

    @Mock
    private MonitoringSnapshotRepository snapshotRepository;

    private CompetitorComparisonService comparisonService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID ownListingId = UUID.randomUUID();
    private final UUID competitorItemId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        comparisonService = new CompetitorComparisonService(listingRepository, watchlistItemRepository, snapshotRepository);
    }

    @Test
    @DisplayName("Should compare own listing with competitor item and identify price position and missing fields")
    void compareOwnListingWithCompetitor() {
        Listing ownListing = new Listing(
                tenantId, UUID.randomUUID(), UUID.randomUUID(), Marketplace.MERCADO_LIVRE,
                "MLB_OWN_1", "SKU-MEU-PROD", "Meu Teclado Gamer Oficial",
                "ACTIVE", new BigDecimal("180.00"), "BRL", 50, 120, "gold_special",
                "https://ml.com/own", "https://img.com/own.jpg",
                br.com.sellerintelligence.sync.domain.DataProvenance.OBSERVED, null, Instant.now()
        );

        WatchlistItem competitorItem = new WatchlistItem(
                UUID.randomUUID(), tenantId, Marketplace.MERCADO_LIVRE, WatchlistItemType.LISTING,
                "MLB_RIVAL_2", "Teclado Concorrente Barato", "https://ml.com/rival", "rival_store", 360
        );

        MonitoringSnapshot latestSnapshot = new MonitoringSnapshot(
                tenantId, competitorItemId, Marketplace.MERCADO_LIVRE, "MLB_RIVAL_2",
                new BigDecimal("200.00"), null, "BRL", "ACTIVE",
                "Teclado Concorrente Barato", "rival_store", "MercadoLíder Gold", true,
                "https://img.com/rival.jpg", CollectionStatus.SUCCESS, null, "API", "OBSERVED", Instant.now()
        );

        when(listingRepository.findByTenantIdAndId(tenantId, ownListingId)).thenReturn(Optional.of(ownListing));
        when(watchlistItemRepository.findByIdAndTenantId(competitorItemId, tenantId)).thenReturn(Optional.of(competitorItem));
        when(snapshotRepository.findFirstByItemIdAndTenantIdOrderByCollectedAtDesc(competitorItemId, tenantId))
                .thenReturn(Optional.of(latestSnapshot));

        CompetitorComparisonService.ComparisonResult result = comparisonService.compare(ownListingId, competitorItemId, tenantId);

        assertThat(result).isNotNull();
        assertThat(result.ownTitle()).isEqualTo("Meu Teclado Gamer Oficial");
        assertThat(result.ownPrice()).isEqualByComparingTo("180.00");
        assertThat(result.competitorPrice()).isEqualByComparingTo("200.00");
        assertThat(result.pricePosition()).isEqualTo("OWN_CHEAPER");
        assertThat(result.priceDeltaAmount()).isEqualByComparingTo("-20.00");
        assertThat(result.priceDeltaPercentage()).isEqualByComparingTo("-10.0000");

        // Verify missing fields disclaimer exists
        assertThat(result.missingFields()).anyMatch(m -> m.contains("Estoque real do concorrente não fornecido"));
        assertThat(result.missingFields()).anyMatch(m -> m.contains("Vendas reais do concorrente não fornecidas"));
    }
}
