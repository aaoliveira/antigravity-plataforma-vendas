package br.com.sellerintelligence.market.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface MarketProductItemRepository extends JpaRepository<MarketProductItem, UUID> {
    List<MarketProductItem> findBySnapshotId(UUID snapshotId);
}
