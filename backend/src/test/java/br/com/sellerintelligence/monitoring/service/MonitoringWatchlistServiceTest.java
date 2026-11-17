package br.com.sellerintelligence.monitoring.service;

import br.com.sellerintelligence.monitoring.domain.*;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonitoringWatchlistServiceTest {

    @Mock
    private WatchlistRepository watchlistRepository;

    @Mock
    private WatchlistItemRepository itemRepository;

    private MonitoringWatchlistService service;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID watchlistId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new MonitoringWatchlistService(watchlistRepository, itemRepository);
    }

    @Test
    @DisplayName("Should create watchlist successfully")
    void createWatchlistSuccessfully() {
        when(watchlistRepository.save(any(Watchlist.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Watchlist created = service.createWatchlist(tenantId, "Concorrentes Periféricos", "Monitoramento de teclados e mouses", WatchlistType.COMPETITOR_LISTING);

        assertThat(created).isNotNull();
        assertThat(created.getName()).isEqualTo("Concorrentes Periféricos");
        assertThat(created.getTenantId()).isEqualTo(tenantId);
        assertThat(created.getStatus()).isEqualTo(WatchlistStatus.ACTIVE);
        assertThat(created.getType()).isEqualTo(WatchlistType.COMPETITOR_LISTING);
    }

    @Test
    @DisplayName("Should add item to watchlist successfully")
    void addItemToWatchlistSuccessfully() {
        when(watchlistRepository.existsByIdAndTenantId(watchlistId, tenantId)).thenReturn(true);
        when(itemRepository.findByWatchlistIdAndMarketplaceAndItemTypeAndExternalId(any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        when(itemRepository.save(any(WatchlistItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WatchlistItem item = service.addItem(
                watchlistId, tenantId, Marketplace.MERCADO_LIVRE, WatchlistItemType.LISTING,
                "MLB987654321", "Teclado Mecânico RGB Switch Blue",
                "https://mercadolivre.com.br/sample", "concorrente_store", 180
        );

        assertThat(item).isNotNull();
        assertThat(item.getExternalId()).isEqualTo("MLB987654321");
        assertThat(item.getMarketplace()).isEqualTo(Marketplace.MERCADO_LIVRE);
        assertThat(item.getStatus()).isEqualTo(WatchlistItemStatus.ACTIVE);
        assertThat(item.getCheckFrequencyMinutes()).isEqualTo(180);
    }

    @Test
    @DisplayName("Untracking item must NOT delete entity or history; sets status UNTRACKED")
    void untrackItemPreservesHistory() {
        WatchlistItem item = new WatchlistItem(
                watchlistId, tenantId, Marketplace.MERCADO_LIVRE, WatchlistItemType.LISTING,
                "MLB987654321", "Item para Pausar", null, null, 360
        );
        UUID itemId = item.getId();

        when(itemRepository.findByIdAndTenantId(itemId, tenantId)).thenReturn(Optional.of(item));
        when(itemRepository.save(any(WatchlistItem.class))).thenAnswer(i -> i.getArgument(0));

        service.untrackItem(itemId, tenantId);

        assertThat(item.getStatus()).isEqualTo(WatchlistItemStatus.UNTRACKED);
        verify(itemRepository, never()).delete(any());
        verify(itemRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Adding existing untracked item reactivates it cleanly")
    void reactivateUntrackedItemOnReAdd() {
        when(watchlistRepository.existsByIdAndTenantId(watchlistId, tenantId)).thenReturn(true);

        WatchlistItem existing = new WatchlistItem(
                watchlistId, tenantId, Marketplace.MERCADO_LIVRE, WatchlistItemType.LISTING,
                "MLB987654321", "Item Antigo", null, null, 360
        );
        existing.untrack();
        assertThat(existing.getStatus()).isEqualTo(WatchlistItemStatus.UNTRACKED);

        when(itemRepository.findByWatchlistIdAndMarketplaceAndItemTypeAndExternalId(
                watchlistId, Marketplace.MERCADO_LIVRE, WatchlistItemType.LISTING, "MLB987654321"))
                .thenReturn(Optional.of(existing));
        when(itemRepository.save(any(WatchlistItem.class))).thenAnswer(i -> i.getArgument(0));

        WatchlistItem reactivated = service.addItem(
                watchlistId, tenantId, Marketplace.MERCADO_LIVRE, WatchlistItemType.LISTING,
                "MLB987654321", "Item Antigo", null, null, 360
        );

        assertThat(reactivated.getStatus()).isEqualTo(WatchlistItemStatus.ACTIVE);
    }
}
