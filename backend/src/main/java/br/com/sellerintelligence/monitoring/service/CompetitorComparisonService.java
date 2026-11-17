package br.com.sellerintelligence.monitoring.service;

import br.com.sellerintelligence.monitoring.domain.MonitoringSnapshot;
import br.com.sellerintelligence.monitoring.domain.MonitoringSnapshotRepository;
import br.com.sellerintelligence.monitoring.domain.WatchlistItem;
import br.com.sellerintelligence.monitoring.domain.WatchlistItemRepository;
import br.com.sellerintelligence.sync.domain.Listing;
import br.com.sellerintelligence.sync.domain.ListingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class CompetitorComparisonService {

    private final ListingRepository listingRepository;
    private final WatchlistItemRepository watchlistItemRepository;
    private final MonitoringSnapshotRepository snapshotRepository;

    public CompetitorComparisonService(ListingRepository listingRepository,
                                       WatchlistItemRepository watchlistItemRepository,
                                       MonitoringSnapshotRepository snapshotRepository) {
        this.listingRepository = listingRepository;
        this.watchlistItemRepository = watchlistItemRepository;
        this.snapshotRepository = snapshotRepository;
    }

    public record ComparisonResult(
            UUID ownListingId,
            String ownTitle,
            String ownSku,
            BigDecimal ownPrice,
            String ownStatus,
            UUID competitorItemId,
            String competitorExternalId,
            String competitorTitle,
            String competitorSeller,
            String competitorReputation,
            BigDecimal competitorPrice,
            String competitorStatus,
            Boolean competitorFreeShipping,
            BigDecimal priceDeltaAmount,
            BigDecimal priceDeltaPercentage,
            String pricePosition, // "OWN_CHEAPER", "OWN_EXPENSIVE", "SAME_PRICE", "UNKNOWN"
            List<String> missingFields,
            String provenance
    ) {}

    @Transactional(readOnly = true)
    public ComparisonResult compare(UUID ownListingId, UUID competitorItemId, UUID tenantId) {
        Listing ownListing = listingRepository.findByTenantIdAndId(tenantId, ownListingId)
                .orElseThrow(() -> new IllegalArgumentException("Own listing not found: " + ownListingId));

        WatchlistItem competitorItem = watchlistItemRepository.findByIdAndTenantId(competitorItemId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Competitor watchlist item not found: " + competitorItemId));

        Optional<MonitoringSnapshot> latestSnapshotOpt = snapshotRepository
                .findFirstByItemIdAndTenantIdOrderByCollectedAtDesc(competitorItemId, tenantId);

        BigDecimal competitorPrice = null;
        String competitorTitle = competitorItem.getTargetName();
        String competitorStatus = competitorItem.getLastStatus();
        String competitorSeller = competitorItem.getSellerNickname();
        String competitorReputation = null;
        Boolean competitorFreeShipping = null;
        String provenance = "OBSERVED";

        List<String> missing = new ArrayList<>();

        if (latestSnapshotOpt.isPresent()) {
            MonitoringSnapshot snap = latestSnapshotOpt.get();
            competitorPrice = snap.getPrice();
            if (snap.getTitle() != null) competitorTitle = snap.getTitle();
            if (snap.getListingStatus() != null) competitorStatus = snap.getListingStatus();
            if (snap.getSellerNickname() != null) competitorSeller = snap.getSellerNickname();
            competitorReputation = snap.getSellerReputation();
            competitorFreeShipping = snap.getFreeShipping();
            provenance = snap.getProvenance();
        } else {
            missing.add("Nenhum snapshot de monitoramento coletado até o momento.");
        }

        if (competitorReputation == null) missing.add("Reputação do vendedor concorrente indisponível.");
        if (competitorFreeShipping == null) missing.add("Condição de frete grátis do concorrente não informada.");
        missing.add("Estoque real do concorrente não fornecido pela API (campo privado / UNAVAILABLE).");
        missing.add("Vendas reais do concorrente não fornecidas pela API (campo privado / UNAVAILABLE).");

        BigDecimal deltaAmount = null;
        BigDecimal deltaPct = null;
        String position = "UNKNOWN";

        if (ownListing.getPrice() != null && competitorPrice != null) {
            deltaAmount = ownListing.getPrice().subtract(competitorPrice).setScale(2, RoundingMode.HALF_EVEN);
            if (competitorPrice.compareTo(BigDecimal.ZERO) > 0) {
                deltaPct = deltaAmount
                        .divide(competitorPrice, 6, RoundingMode.HALF_EVEN)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(4, RoundingMode.HALF_EVEN);
            }
            int comp = ownListing.getPrice().compareTo(competitorPrice);
            if (comp < 0) {
                position = "OWN_CHEAPER";
            } else if (comp > 0) {
                position = "OWN_EXPENSIVE";
            } else {
                position = "SAME_PRICE";
            }
        }

        return new ComparisonResult(
                ownListing.getId(),
                ownListing.getTitle(),
                ownListing.getSku(),
                ownListing.getPrice(),
                ownListing.getStatus(),
                competitorItem.getId(),
                competitorItem.getExternalId(),
                competitorTitle,
                competitorSeller,
                competitorReputation,
                competitorPrice,
                competitorStatus,
                competitorFreeShipping,
                deltaAmount,
                deltaPct,
                position,
                missing,
                provenance
        );
    }
}
