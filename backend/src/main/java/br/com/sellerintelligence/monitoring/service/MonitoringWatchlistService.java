package br.com.sellerintelligence.monitoring.service;

import br.com.sellerintelligence.monitoring.domain.*;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MonitoringWatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final WatchlistItemRepository itemRepository;

    public MonitoringWatchlistService(WatchlistRepository watchlistRepository,
                                      WatchlistItemRepository itemRepository) {
        this.watchlistRepository = watchlistRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional
    public Watchlist createWatchlist(UUID tenantId, String name, String description, WatchlistType type) {
        Watchlist watchlist = new Watchlist(tenantId, name, description, type);
        return watchlistRepository.save(watchlist);
    }

    @Transactional(readOnly = true)
    public List<Watchlist> listWatchlists(UUID tenantId) {
        return watchlistRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    @Transactional(readOnly = true)
    public Optional<Watchlist> getWatchlist(UUID id, UUID tenantId) {
        return watchlistRepository.findByIdAndTenantId(id, tenantId);
    }

    @Transactional
    public Watchlist updateWatchlist(UUID id, UUID tenantId, String name, String description, WatchlistType type) {
        Watchlist watchlist = watchlistRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Watchlist not found: " + id));
        watchlist.update(name, description, type);
        return watchlistRepository.save(watchlist);
    }

    @Transactional
    public void archiveWatchlist(UUID id, UUID tenantId) {
        Watchlist watchlist = watchlistRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Watchlist not found: " + id));
        watchlist.archive();
        watchlistRepository.save(watchlist);
    }

    @Transactional
    public WatchlistItem addItem(UUID watchlistId, UUID tenantId, Marketplace marketplace,
                                 WatchlistItemType itemType, String externalId, String targetName,
                                 String targetUrl, String sellerNickname, int checkFrequencyMinutes) {
        if (!watchlistRepository.existsByIdAndTenantId(watchlistId, tenantId)) {
            throw new IllegalArgumentException("Watchlist not found: " + watchlistId);
        }

        // If item already exists in this watchlist, reactivate it if untracked
        Optional<WatchlistItem> existingOpt = itemRepository
                .findByWatchlistIdAndMarketplaceAndItemTypeAndExternalId(watchlistId, marketplace, itemType, externalId);

        if (existingOpt.isPresent()) {
            WatchlistItem existing = existingOpt.get();
            if (existing.getStatus() == WatchlistItemStatus.UNTRACKED) {
                existing.activate();
                return itemRepository.save(existing);
            }
            return existing;
        }

        WatchlistItem item = new WatchlistItem(
                watchlistId, tenantId, marketplace, itemType,
                externalId, targetName, targetUrl, sellerNickname, checkFrequencyMinutes
        );
        return itemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public List<WatchlistItem> listItems(UUID watchlistId, UUID tenantId) {
        return itemRepository.findByWatchlistIdAndTenantIdOrderByCreatedAtDesc(watchlistId, tenantId);
    }

    @Transactional(readOnly = true)
    public Optional<WatchlistItem> getItem(UUID itemId, UUID tenantId) {
        return itemRepository.findByIdAndTenantId(itemId, tenantId);
    }

    @Transactional
    public void untrackItem(UUID itemId, UUID tenantId) {
        WatchlistItem item = itemRepository.findByIdAndTenantId(itemId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Watchlist item not found: " + itemId));
        // Soft untrack: History of snapshots and events is strictly preserved!
        item.untrack();
        itemRepository.save(item);
    }

    @Transactional
    public void pauseItem(UUID itemId, UUID tenantId) {
        WatchlistItem item = itemRepository.findByIdAndTenantId(itemId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Watchlist item not found: " + itemId));
        item.pause();
        itemRepository.save(item);
    }

    @Transactional
    public void activateItem(UUID itemId, UUID tenantId) {
        WatchlistItem item = itemRepository.findByIdAndTenantId(itemId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Watchlist item not found: " + itemId));
        item.activate();
        itemRepository.save(item);
    }
}
